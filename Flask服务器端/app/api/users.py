from flask import jsonify, g, request, json, url_for, abort
from app import db
from app.api import api
from app.api.authentication import auth
from app.api.errors import forbidden, bad_request
from app.models import User, Article,ReadHistory


# get user info
@api.route('/user/<int:user_id>')
@auth.login_required
def get_user(user_id):
    return User.query.get_or_404(user_id).to_json()


@api.route('/user/', methods=['POST'])
def register_user():
    request_data = request.get_data().decode("UTF-8")
    json_data = json.loads(request_data)
    username = json_data['username']
    email = json_data['email']
    password = json_data['password']
    if username is None or password is None:
        return bad_request('参数不能为空')
    if not User.query.filter_by(username=username).first() is None:
        return bad_request('用户名已存在')
    register_user = User(username=username, email=email, password=password)
    db.session.add(register_user)
    db.session.commit()
    return jsonify({
        'status': 'OK',
        'username': register_user.username,
        'token': register_user.generate_auth_token(expiration=3000),
        'user_id': register_user.id,
        'expiration': 3000
    }), 201, {
               'Location': url_for('.get_user', user_id=register_user.id, _external=True)
           }


@api.route('/user/<int:user_id>/histories/')
@auth.login_required
def get_user_histories(user_id):
    if not g.current_user.id == user_id:
        return forbidden('no watch others info.')
    # read_histories = g.current_user.read_histories.all()
    read_histories = ReadHistory.query.filter_by(user_id=g.current_user.id).order_by(ReadHistory.id.desc()).limit(20).all()
    return jsonify({
        'count': len(read_histories),
        'read_histories': [
            read_history.article.to_json_for_list(5000) for read_history in read_histories
        ]
    })


# 删除阅读历史
@api.route('/user/<int:user_id>/histories/<int:article_id>',methods=['DELETE'])
@auth.login_required
def set_user_histories(user_id,article_id):
    if not g.current_user.id == user_id:
        return forbidden('no watch others info.')
    # 寻找出要删除的文章并删除。
    article_history = ReadHistory.query.filter_by(user_id=g.current_user.id,article_id=article_id).first()
    db.session.delete(article_history)
    db.session.commit()
    response = jsonify({'Info': 'Success', 'Message': 'History deleted.'})
    response.status_code = 204
    return response


# 获取用户的收藏列表
@api.route('/user/<int:user_id>/collections/')
@auth.login_required
def get_user_collections(user_id):
    if not g.current_user.id == user_id:
        return forbidden('no watch others info.')
    articles = User.query.get_or_404(user_id).collections.all()
    num = User.query.get_or_404(user_id).collections.count()
    return jsonify(
        {
            'count': num,
            'collections': [
                article.to_json_for_list(5000) for article in articles
            ]
        }
    )

# 收藏或者取消收藏文章
@api.route('/user/<int:user_id>/collections/<int:article_id>', methods=['POST', 'DELETE'])
@auth.login_required
def set_user_collections(user_id, article_id):
    if not g.current_user.id == user_id:
        return forbidden('不准替别人操作')
    # 找到对应的文章
    article = Article.query.get_or_404(article_id)
    response = ''
    if request.method == 'POST':
        response = jsonify({'info': 'success', 'message': 'created.'})
        response.status_code = 201
        try:
            g.current_user.collect(article)
        except:
            return response
    elif request.method == 'DELETE':
        response = jsonify({'info': 'success', 'message': 'deleted.'})
        response.status_code = 204
        g.current_user.uncollect(article)
    return response


# 设置词汇量
@api.route('/user/<int:user_id>/set-vocabulary/<int:vocabulary>', methods=['POST'])
@auth.login_required
def set_user_vocabulary(user_id, vocabulary):
    if not g.current_user.id == user_id:
        return forbidden('不准替别人操作')
    g.current_user.vocabulary = vocabulary
    db.session.add(g.current_user)
    db.session.commit()

    response = jsonify({'stauts': 'OK', 'message': 'created.'})
    response.status_code = 201
    return response


@api.route('/user/<int:user_id>/followed/')
@auth.login_required
def get_user_followers(user_id):
    this_user = User.query.get_or_404(user_id)
    this_user_name = this_user.username
    users = this_user.followed_users.all()
    return jsonify(
        {
            'current_user': this_user.username,
            'followed_users': [user.to_json() for user in users]
        }
    )
