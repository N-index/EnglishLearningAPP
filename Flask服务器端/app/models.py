import re
from datetime import datetime
import requests
import xlrd as xlrd
from bs4 import BeautifulSoup
from sqlalchemy import or_, and_, event
from sqlalchemy.orm import backref
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import UserMixin, AnonymousUserMixin
from itsdangerous import TimedJSONWebSignatureSerializer as Serializer
from flask import current_app, url_for
from app.exceptions import ValidationError
from . import db, login_manager


# 注册加载用户的方法，供flask-login使用
@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))


# 这个多对多关系由SQLAlchemy维护，不能添加自定义字段。
collections = db.Table('collections',
                       db.Column('user_id', db.Integer, db.ForeignKey('users.id')),
                       db.Column('article_id', db.Integer, db.ForeignKey('articles.id')),
                       db.UniqueConstraint('user_id', 'article_id')
                       )


class ReadHistory(db.Model):
    __tablename__ = 'read_history'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    article_id = db.Column(db.Integer, db.ForeignKey('articles.id'), nullable=False)
    distribution_time = db.Column(db.DateTime, default=datetime.utcnow)
    word_number = db.Column(db.Integer)
    number_of_known = db.Column(db.Integer)
    number_of_unknown = db.Column(db.Integer)

    reader = db.relationship('User', backref='read_histories')
    article = db.relationship('Article', backref='read_histories')

    # reader = db.relationship('User', backref=backref('read_histories',order_by('ReadHistory.id.desc()')))
    # article = db.relationship('Article', backref=backref('read_histories',order_by('ReadHistory.id.desc()')))

    # articles = db.relationship("Article", backref='history')


# 用户关注多对多关系表，不能像上面那个collections表了，这个表我们高度自定义。
class Follow(db.Model):
    __tablename__ = 'follows'
    follower_id = db.Column(db.Integer, db.ForeignKey('users.id'), primary_key=True)
    followed_id = db.Column(db.Integer, db.ForeignKey('users.id'), primary_key=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)


class User(UserMixin, db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(64), unique=True, index=True)
    email = db.Column(db.String(64))
    password_hash = db.Column(db.String(128))
    confirmed = db.Column(db.Boolean, default=False)
    role_id = db.Column(db.Integer, db.ForeignKey('roles.id'), nullable=False)
    vocabulary = db.Column(db.Integer, default=3000)
    member_since = db.Column(db.DateTime(), default=datetime.utcnow)

    # 收藏文章多对多关系中，用户处于主动态
    collections = db.relationship('Article',
                                  secondary=collections,
                                  backref=db.backref('users', lazy='dynamic'),
                                  lazy='dynamic')


    def __init__(self, **kwargs):
        super(User, self).__init__(**kwargs)
        if self.role is None:
            # 用官方邮箱注册的账号默认天龙人
            if self.email == current_app.config['MAIL_USERNAME']:
                self.role = Role.query.filter_by(name='Administrator').first()
            # 小鱼小虾都是默认角色User
            if self.role is None:
                # Role中设置default字段就是这样派用场的。因为role的实例分布为绝大多数都是User，知乎应该把小管家也索引出来了。
                self.role = Role.query.filter_by(default=True).first()

    @property
    def followed_users(self):
        # 返回followed的用户列表
        return User.query.join(Follow, Follow.followed_id == User.id).filter(Follow.follower_id == self.id)

    # 应当使用异步HTTP来关注取关。收藏文章也是。
    def follow(self, user):
        if not self.is_following(user):
            f = Follow(followed=user)
            self.followed.append(f)
            db.session.add(self)
            db.session.commit()

    def unfollow(self, user):
        f = self.followed.filter_by(followed_id=user.id).first()
        if f:
            self.followed.remove(f)
            db.session.add(self)
            db.session.commit()

    # 用户收藏文章。
    def collect(self, article):
        self.collections.append(article)
        db.session.add(self)
        db.session.commit()

    # 用户取消收藏文章。
    def uncollect(self, article):
        self.collections.remove(article)
        db.session.add(self)
        db.session.commit()

    # 判断当前用户有没有关注user
    def is_following(self, user):
        # 判断有没有这个用户
        if user.id is None:
            return False
        # self.followed筛选出Follow表里当前用户的用户关注列表，
        # filter_by(followed_id=user,id)在用户关注列表中查询是否有 被关注者id为user.id的。
        return self.followed.filter_by(followed_id=user.id).first() is not None

    # 原理同上
    def is_followed_by(self, user):
        if user.id is None:
            return False
        return self.followers.filter_by(followed_id=user.id).first() is not None

    def can(self, perm):
        # 我其实不知道为什么这里要判断一下用户角色是否为None，怎么会有这样的情况存在呢。
        if self.role is not None and self.role.has_permission(perm):
            return True

    def is_administrator(self):
        # 这里再次印证分级定义权限的优越性，相互独立互不干扰。如果是我设计权限系统，那我指定懵逼。
        return self.can(Permission.ADMIN)

    @property
    def password(self):
        raise AttributeError('Password is not a readable attribute.')

    @password.setter
    def password(self, password):
        self.password_hash = generate_password_hash(password)

    def verify_password(self, password):
        """ return : True or False """
        return check_password_hash(self.password_hash, password)

    # 获取认证Token
    def generate_confirmation_token(self, expires_in=3600):
        s = Serializer(current_app.config['SECRET_KEY'], expires_in=expires_in)
        return s.dumps({'confirm': self.id}).decode('utf-8')

    # 根据Token认证用户，确认current_user
    def confirm(self, token):
        s = Serializer(current_app.config['SECRET_KEY'])
        try:
            data = s.loads(token.encode('utf-8'))
        except:
            return False
        if data.get('confirm') != self.id:
            return False
        self.confirmed = True
        db.session.add(self)
        db.session.commit()
        return True

    # 这一对跟上面那一对有上面区别呢？查找到用户。
    # 这个token验证的是用户的id，静态方法，其他用户无法查到。获取用户，后续使这个用户处于登录状态。
    # 这个解码方法是静态方法，因为只有解码令牌后才能知道用户是谁。

    def generate_auth_token(self, expiration):
        s = Serializer(current_app.config['SECRET_KEY'], expires_in=expiration)
        return s.dumps({'id': self.id}).decode('utf-8')

    @staticmethod
    def verify_auth_token(token):
        s = Serializer(current_app.config['SECRET_KEY'])
        try:
            data = s.loads(token)
        except:
            return None
        return User.query.get(data['id'])

    def to_json(self):
        # 注意隐私问题
        json_user = {
            'url': url_for('api.get_user', user_id=self.id),
            'username': self.username,
            'email': self.email,
            'confirmed': self.confirmed,
            'collections': url_for('api.get_user_collections', user_id=self.id),
            'member_since': self.member_since,
            'vocabulary':self.vocabulary
        }
        return json_user

    # 定义可读性的字符串表示模型，供调试和测试使用。
    def __repr__(self):
        return '<User %r>' % self.username


# 匿名用户没有人权，仅可以浏览公开页面。不需要登录，也不需要认证，不需要权限的。
# 再次感叹要是让我设计权限，从一开始我可能 把权限分成无数个等级。杂乱不堪。因为我可能手里拿个锤子，万物都是权限。
class AnonymousUser(AnonymousUserMixin):

    def can(self, permissions):
        return False

    def is_administrator(self):
        return False


login_manager.anonymous_user = AnonymousUser


class Role(db.Model):
    __tablename__ = 'roles'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(64), unique=True)
    default = db.Column(db.Boolean, default=False, index=True)
    permissions = db.Column(db.Integer)
    users = db.relationship('User', backref='role')

    def __init__(self, **kwargs):
        super(Role, self).__init__(**kwargs)
        if self.permissions is None:
            self.permissions = 0

    # 更新或添加角色权限
    @staticmethod
    def insert_roles():
        roles = {
            'User': [Permission.FOLLOW, Permission.COMMENT, Permission.WRITE],
            'Moderator': [Permission.FOLLOW, Permission.COMMENT, Permission.WRITE, Permission.MODERATE],
            'Admin': [Permission.FOLLOW, Permission.COMMENT, Permission.WRITE, Permission.MODERATE, Permission.ADMIN]
        }
        default_role = 'User'
        for r in roles:
            role = Role.query.filter_by(name=r).first()
            if role is None:
                role = Role(name=r)
            # 先初始化当前角色的权限
            role.reset_permission()
            # 为当前角色添加权限
            for perm in roles[r]:
                role.add_permission(perm)
            # 这角色如果是User，那么就把default设为True
            role.default = (role.name == default_role)
            db.session.add(role)
            db.session.commit()

    # 以下四个函数为 权限相关函数。常用于审核系统、成长系统。
    def add_permission(self, perm):
        if not self.has_permission(perm):
            self.permissions += perm

    def remove_permission(self, perm):
        if self.has_permission(perm):
            self.permissions -= perm

    def reset_permission(self):
        self.permissions = 0

    def has_permission(self, perm):
        return self.permissions & perm == perm

    def __repr__(self):
        return '<Role %r>' % self.name


class Article(db.Model):
    __tablename__ = 'articles'
    id = db.Column(db.Integer, primary_key=True)

    sourceID = db.Column(db.String(64))
    sourceName = db.Column(db.String(64))
    title = db.Column(db.String(256), unique=True)
    author = db.Column(db.String(128))
    description = db.Column(db.Text())
    content = db.Column(db.Text())
    category = db.Column(db.String(16))

    # url = db.Column(db.String(128))
    sourceURL = db.Column(db.String(512))
    urlToImage = db.Column(db.Text())
    publish_time = db.Column(db.DateTime())
    storage_time = db.Column(db.DateTime(), default=datetime.utcnow)
    word_number = db.Column(db.Integer)
    content_completed_status = db.Column(db.Boolean, default=False)
    forbidden = db.Column(db.Boolean, default=False)

    def to_json(self, translate=None, number_of_unknown=0):
        content = translate if translate else self.content
        json_article = {
            'id': self.id,
            'title': self.title,
            'count': self.word_number,
            'content': content,
            'author': self.author,
            'URL': url_for('api.get_article', article_id=self.id),
            'sourceURL': self.sourceURL,
            'urlToImage': self.urlToImage,
            'publish_time': self.publish_time,
            'category': self.category,
            'number_of_unknown':number_of_unknown
        }
        return json_article

    def to_json_for_list(self, count_of_new_word):
        json_article = {
            'id': self.id,
            'sourceName': self.sourceName,
            'title': self.title,
            'count': self.word_number,
            'author': self.author,
            'description': self.description,
            'URL': url_for('api.get_article', article_id=self.id),
            'sourceURL': self.sourceURL,
            'urlToImage': self.urlToImage,
            'publish_time': self.publish_time,
            'count_of_new_word': count_of_new_word,
            'category': self.category,

        }
        return json_article

    # 禁止文章
    def set_forbidden(self):
        self.forbidden = True
        db.session.add(self)
        db.session.commit(self)

    # reutersFeeds =Article.query.filter(Article.sourceName ==
    # 'reuters' or Article.sourceName == 'Reuters',Article.sourceURL.contains('feeds.reuters.com')).all()
    # 修订数据库中所有路透社的feeds内容
    @staticmethod
    def set_full_content_for_all():
        reutersArticles = Article.query.filter(
            and_(or_(Article.content_completed_status is False, Article.content_completed_status == 0),
                 or_(Article.sourceName == 'reuters', Article.sourceName == 'Reuters')
                 )
        ).all()
        for article in reutersArticles:
            article.set_content()
        print('修订完毕')

    # 文章入库
    @staticmethod
    def from_json(json_article, category='general'):
        source = json_article.get('source')
        sourceID = ''
        sourceName = ''
        if source:
            sourceID = source.get('id') or ''
            sourceName = source.get('name') or ''
        title = json_article.get('title') or ''
        content = json_article.get('content') or ''
        author = json_article.get('author') or ''
        if len(author) > 63:
            author = author[:63]
        description = json_article.get('description') or ''
        sourceURL = json_article.get('url') or ''
        word_number = 0
        content_completed_status = False
        if len(sourceURL) > 255:
            sourceURL = 'too long'
        if sourceID in ['reuters', 'Reuters'] and not bool(content_completed_status):
            print('正在为当前新闻填充完整内容...', end=' ')
            origin_url = sourceURL
            if 'feeds.reuters.com' in sourceURL:
                # 禁止requests自动处理重定向
                redirection = requests.get(sourceURL, allow_redirects=False)
                origin_url = redirection.headers['Location']
                sourceURL = origin_url
            html = requests.get(origin_url).text
            # 清洗数据
            bs = BeautifulSoup(html, features='html.parser')
            res = ''
            try:
                article = bs.find('div', {'class': 'StandardArticleBody_body'}).find_all('p')
                for i in article:
                    res += '\n' + i.text
            except:
                pass
            content = res
            content_completed_status = True
            word_number = len(re.findall(r'\w+', res))
            print('当前新闻内容填充完毕', end=' ')
        else:
            print('当前sourceURL不可解析', end=' ')
        urlToImage = json_article.get('urlToImage') or ''
        publish_time = datetime.utcnow()
        if json_article.get('publishedAt'):
            publish_time = datetime.strptime(json_article.get('publishedAt')[:19], '%Y-%m-%dT%H:%M:%S')
        category = category
        return Article(sourceID=sourceID,
                       sourceName=sourceName,
                       title=title,
                       content=content,
                       author=author,
                       description=description,
                       sourceURL=sourceURL,
                       urlToImage=urlToImage,
                       publish_time=publish_time,
                       category=category,
                       content_completed_status=content_completed_status,
                       word_number=word_number
                       )


# db.event.listen(Post.body,'set',Post.receive_set)


# https://docs.sqlalchemy.org/en/13/orm/events.html
# @event.listens_for(Article, 'after_insert')
# def receive_after_insert(mapper, connection, target):
#     target.set_content()


# 涉及sourceURL的重置，所以不用set了，会无限触发事件。解决方法：事件触发中增加判断，如果sourceURL判定为已经重置过，那么直接return退出事件触发。但是为了保持简洁性，选用insert，而且有一个额外的好处就是避免处理重复文章。
# 想到这里回过神来，不会after_insert也会无限触发吧。更新record的部分column的信息不算insert，我就放心了。
# @event.listens_for(Article.sourceURL, 'set')
# def receive_set(target, value, oldvalue, initiator):
#     target.set_content(value,oldvalue)


# db.event.listen(Article, 'after_insert', Article.set_content_for_single)


class Newword(db.Model):
    __tablename = 'newwords'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    word = db.Column(db.String(64), nullable=False)
    added_time = db.Column(db.DateTime, default=datetime.utcnow)

    def to_json(self):
        json_newword = {
            'id': self.id,
            'user_id': self.user_id,
            'word': self.word
        }
        return json_newword


class AppStatus(db.Model):
    __tablename__ = 'app_status'
    id = db.Column(db.Integer, primary_key=True)
    run_days = db.Column(db.BigInteger, default=0)
    scrape_times = db.Column(db.BigInteger, default=0)
    latest_update_time = db.Column(db.DateTime(), default=datetime.utcnow)


class ScrapeInfo(db.Model):
    __tablename__ = 'scrape_info'
    id = db.Column(db.Integer, primary_key=True)
    total_count = db.Column(db.Integer, default=0)
    scraper_category = db.Column(db.String(64))
    general_count = db.Column(db.Integer, default=0)
    business_count = db.Column(db.Integer, default=0)
    entertainment_count = db.Column(db.Integer, default=0)
    health_count = db.Column(db.Integer, default=0)
    science_count = db.Column(db.Integer, default=0)
    technology_count = db.Column(db.Integer, default=0)
    sports_count = db.Column(db.Integer, default=0)
    time = db.Column(db.DateTime, default=datetime.utcnow)


class WordList(db.Model):
    __tablename__ = 'word_list'
    id = db.Column(db.Integer, primary_key=True)
    word = db.Column(db.String(64), nullable=False)

    @staticmethod
    def excel2mysql():
        file = 'D:\\Desktop\\allWords.xlsx'
        work_book = xlrd.open_workbook(file)
        sheet = work_book.sheet_by_index(1)
        for i in range(1, sheet.nrows):
            word = WordList(word=sheet.cell_value(i, 3))
            db.session.add(word)
        db.session.commit()


class Permission:
    FOLLOW = 1
    COMMENT = 2
    WRITE = 4
    MODERATE = 8
    ADMIN = 16
