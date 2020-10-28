from flask import jsonify, g, request,json
from app import db
from app.api import api
from app.api.authentication import auth
from app.api.errors import forbidden, bad_request
from app.models import Newword


# get user newwords info.
@api.route('/user/<int:user_id>/newwords/')
@auth.login_required
def get_new_words(user_id):
    if not g.current_user.id == user_id:
        return forbidden('no watch others newwords info.')
    newwords = Newword.query.filter_by(user_id=g.current_user.id).order_by(Newword.id.desc()).all()
    num = Newword.query.filter_by(user_id=g.current_user.id).count()
    return jsonify(
        {
            'count': num,
            'newwords': [
                newword.to_json() for newword in newwords
            ]
        }
    )


# 添加生词，删除生词
@api.route('/user/<int:user_id>/newwords/', methods=['POST', 'DELETE'])
@auth.login_required
def set_user_new_words(user_id):
    if not g.current_user.id == user_id:
        return forbidden('不准替别人操作')
    response = ''
    if request.method == 'POST':
        newword = Newword(user_id=g.current_user.id, word=request.json.get('word'))
        response = jsonify({'Info': 'Success', 'Message': 'New word added.'})
        response.status_code = 201
        db.session.add(newword)
        db.session.commit()
    elif request.method == 'DELETE':
        response = jsonify({'Info': 'Success', 'Message': 'Word deleted.'})
        request_data = request.get_data().decode("UTF-8")
        json_data = json.loads(request_data)
        new_word_id = json_data.get('id')

        if new_word_id:
            newword = Newword.query.get_or_404(new_word_id)
            response.status_code = 201
            db.session.delete(newword)
            db.session.commit()
            return response
        else:
            return bad_request('Param wrong.')
    return response
