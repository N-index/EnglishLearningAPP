import os


# 基础配置
class Config:
    # 表单加密需要 跨站请求伪造保护
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'peofialz'
    # 邮件SMTP配置
    MAIL_SERVER = 'smtp.gmail.com'
    MAIL_PORT = 465
    MAIL_USE_TLS = False
    MAIL_USE_SSL = True
    MAIL_USERNAME = os.environ.get('MAIL_USERNAME')
    MAIL_PASSWORD = os.environ.get('MAIL_PASSWORD')
    FLASKY_MAIL_SUBJECT_PREFIX = 'English Learning'
    FLASKY_MAIL_SENDER = 'FLASKY Admin'
    # 数据库配置
    SQLALCHEMY_COMMIT_ON_TEARDOWN = False
    SQLALCHEMY_TRACK_MODIFICATIONS = False  # 动态追踪修改设置，如未设置会Warning
    SQLALCHEMY_ECHO = False  # 查询时是否显示原始SQL语句
    SQLALCHEMY_RECORD_QUERIES = True
    FLASKY_SLOW_DB_QUERY_TIME = 0.5

    NEWS_API_KEY = os.environ.get('NEWS_API_KEY') 
    SCHEDULER_API_ENABLED = True



    @staticmethod
    def init_app(app):
        pass


# 继承Config
class DevelopmentConfig(Config):
    DEBUG = True
    SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://root:123456@localhost:3306/DEV-relaxEnglish?charset=UTF8MB4'


class TestingConfig(Config):
    TESTING = True
    SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://root:123456@localhost:3306/TEST-relaxEnglish?charset=UTF8MB4'


class ProductionConfig(Config):
    DEBUG = False
    SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://root:123456@localhost:3306/relaxEnglish?charset=UTF8MB4'

    @classmethod
    def init_app(cls, app):
        Config.init_app(app)
        # 出错时邮件通知管理员
        import logging
        from logging.handlers import SMTPHandler
        credentials = None
        secure = None
        if getattr(cls, 'MAIL_USERNAME', None) is not None:
            credentials = (cls.MAIL_USERNAME, cls.MAIL_PASSWORD)
            if getattr(cls, 'MAIL_USE_TLS', None):
                secure = ()
        mail_handler = SMTPHandler(
            mailhost=(cls.MAIL_SERVER, cls.MAIL_PORT),
            fromaddr=cls.FLASKY_MAIL_SENDER,
            toaddrs=[cls.MAIL_USERNAME],
            subject=cls.FLASKY_MAIL_SUBJECT_PREFIX + ' Application Error',
            credentials=credentials,
            secure=secure
        )
        mail_handler.setLevel(logging.ERROR)
        app.logger.addHandler(mail_handler)


config = {
    'development': DevelopmentConfig,
    'testing': TestingConfig,
    'production': ProductionConfig,

    'default': DevelopmentConfig
}
