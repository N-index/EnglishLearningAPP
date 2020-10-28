from flask import request, jsonify, render_template
from app.api import api
from app.exceptions import ValidationError


# 此error.py不承担太多功能，只集成一些简单的API错误处理及消息分发，并不拦截各种错误请求


# API中ValidationError异常的处理程序
@api.errorhandler(ValidationError)
def validation_error(e):
    return bad_request(e.args[0])


def bad_request(message):
    response = jsonify({'status': 'error', 'message': message})
    response.status_code = 400
    return response


def page_not_found(message):
    response = jsonify({'status': 'error', 'message': message})
    response.status_code = 404
    return response


def forbidden(message):
    response = jsonify({'error': 'forbidden', 'message': message})
    response.status_code = 403
    return response


def unauthorized(message):
    response = jsonify({'error': 'unauthorized', 'message': message})
    response.status_code = 401
    return response
