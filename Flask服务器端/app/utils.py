import asyncio
import hashlib
import urllib.parse
import nltk
# from aiohttp.web_exceptions import HTTPError
from flask import json
from nltk.corpus import wordnet
from aiohttp import ClientSession



def get_word_pos(word):
    '''Map POS tag to first character lemmatize() accepts'''
    tag = nltk.pos_tag([word])[0][1][0].upper()
    tag_dict = {"J": wordnet.ADJ,
                "N": wordnet.NOUN,
                "V": wordnet.VERB,
                "R": wordnet.ADV}
    return tag_dict.get(tag, wordnet.NOUN)


def md5(key_for_md5):
    m = hashlib.md5()
    m.update(key_for_md5.encode("utf8"))
    return m.hexdigest()


# 单次请求
async def get_translate(word, session):
    url = "http://fanyi.sogou.com:80/reventondc/api/sogouTranslate"
    pid = "123456"
    key = "123456"
    salt = "1508404016012"  # 随机数，可以填入时间戳
    q = word
    sign = md5(pid + q + salt + key)
    fromL, to = 'en', "zh-CHS"
    payload = "from=" + fromL + "&to=" + to + "&pid=" + pid + "&q=" + urllib.parse.quote(
        q) + "&sign=" + sign + "&salt=" + salt + "&dict=true"
    headers = {
        'content-type': "application/x-www-form-urlencoded",
        'accept': "application/json"
    }
    async with session.request("POST", url, data=payload, headers=headers) as response:
        # return await response.json(content_type='application/json')
        return await response.json(content_type=None)


async def job(word, dictMode=False):
    async with ClientSession() as session:
        json_content = await get_translate(word, session)
        # return json_content.get('usual')[0]['values']
        if not dictMode:
            return json_content.get('translation')
        return json_content



