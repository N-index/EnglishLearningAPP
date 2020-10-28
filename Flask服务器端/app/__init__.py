from flask import Flask
from flask_bootstrap import Bootstrap
from flask_mail import Mail
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
from flask_apscheduler import APScheduler
from config import config

bootstrap = Bootstrap()
mail = Mail()
scheduler = APScheduler()
db = SQLAlchemy()
login_manager = LoginManager()
login_manager.login_view = 'auth.login'


# 工厂函数，参数为config名字，如development,production,testing
def create_app(config_name):
    app = Flask(__name__)
    app.config.from_object(config[config_name])
    config[config_name].init_app(app)

    # 根据APP的config参数进行实例化。所以如果意图在shell命令中调控其他DB，那么需要在config里改。
    bootstrap.init_app(app)
    mail.init_app(app)
    db.init_app(app)
    login_manager.init_app(app)
    scheduler.init_app(app)



    # 添加路由和自定义的错误页面
    from .main import main as main_blueprint
    app.register_blueprint(main_blueprint)

    from .auth import auth as auth_blueprint
    app.register_blueprint(auth_blueprint, url_prefix='/auth')

    from .api import api as api_blueprint
    app.register_blueprint(api_blueprint, url_prefix='/api/v1')

    return app
