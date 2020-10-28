from flask import Blueprint

api = Blueprint('api', __name__)

from . import users, articles, authentication, errors, newwords, translation
