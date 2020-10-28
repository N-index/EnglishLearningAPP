import unittest
from flask import current_app
from app import create_app, db


class BasicsTestCase(unittest.TestCase):
    # 此方法尝试创建一个测试环境。
    def setUp(self):
        self.app = create_app('testing')
        # 创建上下文
        self.app_context = self.app.app_context()
        self.app_context.push()
        db.create_all()

    def tearDown(self):
        # 毁灭测试环境
        db.session.remove()
        db.drop_all()
        self.app_context.pop()

    # 测试APP是否存在
    def test_app_exists(self):
        self.assertFalse(current_app is None)

    # 测试当前APP是否处于TESTING模式
    def test_app_is_testing(self):
        self.assertTrue(current_app.config['TESTING'])

# if __name__ == '__main__':
#     unittest.main()
