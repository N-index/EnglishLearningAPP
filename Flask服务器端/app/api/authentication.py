from flask import g, jsonify, request, json
from flask_httpauth import HTTPBasicAuth
from app.models import User
from . import api
from .errors import unauthorized, forbidden

# HTTPAuth只在api蓝本中使用，所以不在全局注册
from .. import db

auth = HTTPBasicAuth()


# 初始化Flask-HTTPAuth扩展
@auth.verify_password
def verify_password(email_or_token, password):
    # 既没email，也没token
    if email_or_token == '':
        return False
    # 密码为空，使用token登录
    if password == '':
        # 如果token失效，则返回 None
        g.current_user = User.verify_auth_token(email_or_token)
        # 证明当前用户认证使用的是token
        g.token_used = True
        # 不为None则用户认证成功
        return g.current_user is not None
    # 根据邮箱查到用户
    user = User.query.filter_by(email=email_or_token).first()
    if not user:
        return False
    g.current_user = user
    # 第一次访问，使用密码验证
    g.token_used = False
    return user.verify_password(password)


@auth.error_handler
def auth_error():
    return unauthorized('Invalid credentials.@bigpoker')


# 这个蓝本中所有路由都要使用相同的方式进行保护,所以：
# 用户必须登录，并且账号已经激活。
@api.before_request
def before_request():
    pass
    # if not g.current_user.is_anonymous and \
    #         not g.current_user.confirmed:
    #     return forbidden('Unconfirmed account.')


@api.route('/tokens/', methods=['POST'])
@auth.login_required
def get_token():
    # 防止用户使用旧token请求新token.
    if g.current_user.is_anonymous or g.token_used:
        return unauthorized('不准无限续命')
    return jsonify(
        {
            'token': g.current_user.generate_auth_token(expiration=3000),
            'expiration': 3000,
            'user_id':g.current_user.id,
            'username':g.current_user.username
        }
    )
