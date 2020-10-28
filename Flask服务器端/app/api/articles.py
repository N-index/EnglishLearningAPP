import asyncio
import nltk
from flask import request, jsonify, url_for, g
from app import db
from app.api import api
from app.api.authentication import auth
from app.api.errors import forbidden, page_not_found, bad_request
from app.api.decorators import permission_required
from app.models import Article, Permission, WordList, ReadHistory
from nltk.stem import WordNetLemmatizer
from app.utils import get_word_pos, job


# Single article
@api.route('/article/<int:article_id>')
@auth.login_required
def get_article(article_id):
    article = Article.query.get_or_404(article_id, description='Make sure URL right.')
    article_content = str(article.content)
    # 用户已掌握的单词
    familiar_words = [word.word for word in WordList.query.limit(g.current_user.vocabulary).all()]
    lemmatizer = WordNetLemmatizer()
    number_of_known, number_of_unknown = 0, 0
    word_tokens = nltk.word_tokenize(article_content)
    waiting_for_query = []
    for i, w in enumerate(word_tokens):
        pos = get_word_pos(w)
        # 对word进行 lemmatize
        if lemmatizer.lemmatize(w.lower(), pos) not in familiar_words and w.isalpha() and len(w) > 2:
            waiting_for_query.append((i, w))
            number_of_known += 1
        else:
            number_of_unknown += 1
    # 异步查询单词释义
    word_list = [w for (i, w) in waiting_for_query]
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    tasks = [job(word) for word in word_list]
    query_res = loop.run_until_complete(asyncio.gather(*tasks))
    loop.close()
    
    # 在特定位置替换出查询后的结果
    k = 0
    for index in [i for (i, w) in waiting_for_query]:
        word_tokens[index] += '(%s)' % query_res[k]
        k += 1
    content = ' '.join(word_tokens)
    read_history = ReadHistory(word_number=article.word_number,
                               number_of_known=number_of_known,
                               number_of_unknown=number_of_unknown)
    read_history.article = article
    read_history.reader = g.current_user
    db.session.add(read_history)
    db.session.commit()
    return article.to_json(content,number_of_unknown)


# 获取最新新闻
@api.route('/articles/')
@auth.login_required
def get_latest_news():
    articles = Article.query.filter_by(sourceName='reuters' or 'Reuters').order_by(Article.publish_time.desc()).limit(
        20).all()
    # articles = Article.query.order_by(Article.publish_time.desc()).limit(20).all()
    count_of_new_word = g.current_user.vocabulary
    return jsonify({
        'articles': [article.to_json_for_list(count_of_new_word) for article in articles]
    })

