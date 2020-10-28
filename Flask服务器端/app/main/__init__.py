from flask import Blueprint

# 实例化main蓝本，第1个参数是蓝本的名字、第二个参数是蓝本所在的模块。
main = Blueprint('main', __name__)

# 导入view和errors可以把路由与错误处理与蓝本关联起来。
# 在末尾导入，避免循环导入依赖，因为views和errors还要引入main蓝本
from . import views, errors
