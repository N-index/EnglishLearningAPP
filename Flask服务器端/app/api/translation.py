import asyncio

from flask import request, json, g,jsonify

from app import db
from app.api import api
from app.api.authentication import auth
from app.models import Newword,WordList
from app.utils import job


# 还没写好json的遍历解析出wordlist
@api.route('/translation/', methods=['POST'])
@auth.login_required
def get_translation():
    request_data = request.get_data().decode("UTF-8")
    json_data = json.loads(request_data)
    word_list = json_data.get('word_list')
    for word in word_list:
        newword = Newword(user_id=g.current_user.id, word=word)
        db.session.add(newword)
    db.session.commit()

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    tasks = [job(word, True) for word in word_list]
    query_res = loop.run_until_complete(asyncio.gather(*tasks))
    loop.close()
    print(query_res)
    return jsonify(query_res)
    # return str(query_res)

@api.route('/vocab-example/<int:number>',methods=['GET'])
def get_vocabulary_example(number):
    word_list = WordList.query.paginate(number,100).items[:10]

    return jsonify({
        "example":
                [ wordRow.word for wordRow in word_list]
        }),200

