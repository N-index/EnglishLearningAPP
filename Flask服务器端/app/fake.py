from random import randint
from app import db
from faker import Faker
from app.models import Article, User, Role


#  添加虚拟用户
def insert_users():
    faker = Faker(['zh-CN', 'en_US'])
    for _ in range(100):
        db.session.add(
            User(
                username=faker.name(),
                email=faker.email(),
                role=Role.query.filter_by(default=True).first(),
                about_me=faker.sentence(10),
                member_since=faker.past_datetime(start_date='-366d', tzinfo=None)
            )
        )
        try:
            db.session.commit()
        except:
            db.session.rollback()


# 添加虚拟文章
def insert_articles():
    faker = Faker(['zh-CN', 'en_US'])
    for _ in range(125):
        article = Article(title=faker.sentence(6),
                          author=faker.name(),
                          body=faker.text(max_nb_chars=2000, ext_word_list=None),
                          publish_time=faker.past_datetime(start_date='-800d', tzinfo=None)
                          )
        db.session.add(article)
        db.session.commit()


# 添加虚拟关系
def insert_follows():
    user_count = User.query.count()
    # follow_set = set()
    for _ in range(170):
        id1 = randint(1, user_count)
        id2 = randint(1, user_count)
        u1 = User.query.filter_by(id=id1).first()
        u2 = User.query.filter_by(id=id2).first()
        u1.followed.append(u2)
        db.session.add(u1)
        try:
            db.session.commit()
        except:
            db.session.rollback()


# 添加用户收藏文章的记录,因为collections这个表不是我维护的，所以只能通过用户收藏文章。即：user1.collections.append(article1)
def insert_collections():
    user_count = User.query.count()
    article_count = Article.query.count()
    # collection_set = set()
    for _ in range(70):

        user_id = randint(1, user_count)
        print('总用户数量:' + str(user_count))
        print('随机选出的用户id:' + str(user_id))

        article_id = randint(1, article_count)
        print('总文章数量:' + str(article_count))
        print('随机选出的文章id:' + str(article_id))

        # 检测是否重复
        # while (user_id, article_id) in collection_set or (user_id == article_id):
        #     user_id = randint(1, user_count)
        #     article_id = randint(1, article_count)
        # collection_set.add((user_id, article_id))
        # 找出一个用户
        user1 = User.query.filter_by(id=user_id).first()
        print('当前用户：')
        print(user1, user1.id)
        # 找出一篇文章
        print('当前文章：')
        article1 = Article.query.filter_by(id=article_id).first()
        print(article1, article1.id)
        print('准备添加:')
        user1.collections.append(article1)
        db.session.add(user1)
        try:
            db.session.commit()
        except:
            db.session.rollback()
        print('添加完毕。')
