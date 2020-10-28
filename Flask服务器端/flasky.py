import os
from datetime import date, datetime

from newsapi.newsapi_client import NewsApiClient
from app import create_app, db, scheduler
from app.fake import insert_follows, insert_collections
from app.models import User, Role, Article, Follow, ScrapeInfo, AppStatus, WordList, ReadHistory
from flask_migrate import Migrate, upgrade

# 通过配置创建应用。
app = create_app(os.getenv('FLASK_CONFIG') or 'default')
migrate = Migrate(app, db)


# 在flask shell中注册db、User、Role，如果有其他需求，还可以添东西。
@app.shell_context_processor
def make_shell_context():
    return dict(db=db,
                User=User,
                Role=Role,
                Article=Article,
                Follow=Follow,
                WordList=WordList,
                ReadHistory=ReadHistory,
                insert_follows=insert_follows,
                insert_collections=insert_collections,
                headline_all_category=headline_all_category,
                scrape_everything=scrape_everything)


# 批量入库文章，返回入库文章总数
def storage_to_database(articles, message, category='general'):
    total_num = len(articles.get('articles'))
    current_index = 1
    print('*一共获取到%d条新闻,即将开始存储*' % total_num)
    valid_num, invalid_num = 0, 0
    for articleJson in articles.get('articles'):
        print(' 第%d条：' % current_index, end=' ')
        current_index += 1
        article = Article.from_json(articleJson, category=category)
        try:
            db.session.add(article)
            db.session.commit()
            valid_num += 1
            print('现入库一条【' + message + '】的新闻(%s)' % article.title)
        except:
            db.session.rollback()
            invalid_num += 1
            print('此条新闻重复,不予入库(%s)' % article.title)
    return valid_num


# 此函数为抓取【headline】的最小单位，将爬虫与应用程序及数据库连接起来。
def headline_scraper(category=None, country='us', page_size=100, page=1):
    print(str(category) + ' headline scraper is working...')
    news_api = NewsApiClient(api_key='961ea064367244adaa2689122857b3d0')
    top_headlines = news_api.get_top_headlines(page_size=page_size, page=page, country=country, category=category)
    num = storage_to_database(top_headlines, message='%s类别' % str(category), category=category)
    print('【%d】条【%s】类别的热门头条已抓取完毕' % (num, str(category)))
    return num


# 头条文章集合体，按照【类别】和【国家】，有效文章的浓度低
def headline_all_category():
    with app.app_context():
        current_status = AppStatus.query.get(1)
        current_scrape_times = current_status.scrape_times + 1
        print('---------第%s轮下载开始---------' % current_scrape_times)
        general_count = headline_scraper()
        business_count = headline_scraper(category='business', country='us')
        entertainment_count = headline_scraper(category='entertainment', country='us')
        health_count = headline_scraper(category='health', country='us')
        science_count = headline_scraper(category='science', country='us')
        technology_count = headline_scraper(category='technology', country='us')
        sports_count = headline_scraper(category='sports', country='us')
        total_count = general_count + business_count + entertainment_count + health_count + science_count + technology_count + sports_count
        latest_scrape = ScrapeInfo(total_count=total_count,
                                   scraper_category='headline',
                                   general_count=general_count,
                                   business_count=business_count,
                                   entertainment_count=entertainment_count,
                                   health_count=health_count,
                                   science_count=science_count,
                                   technology_count=technology_count,
                                   sports_count=sports_count)
        db.session.add(latest_scrape)
        db.session.commit()
        print('---------第%s轮下载完毕---------' % current_scrape_times)
        current_status.scrape_times = current_scrape_times
        db.session.add(current_status)
        db.session.commit()

# 头条文章集合体，按照【来源】
def headline_by_sources():
    with app.app_context():
        current_status = AppStatus.query.get(1)
        current_scrape_times = current_status.scrape_times + 1
        print('---------第%s轮下载开始---------' % current_scrape_times)
        general_count = headline_scraper_by_sources('reuters')

        total_count = general_count
        latest_scrape = ScrapeInfo(total_count=total_count,
                                   scraper_category='headline_by_sources',
                                   general_count=general_count)
        db.session.add(latest_scrape)
        db.session.commit()
        print('---------第%s轮下载完毕---------' % current_scrape_times)
        current_status.scrape_times = current_scrape_times
        db.session.add(current_status)
        db.session.commit()


# date,固定时间执行一次，参数为date或datetime；
# cron,周期性执行任务，参数为年月日时分秒；
# interval,间接性执行任务，参数为时间间隔；
# 2小时抓取一次
scheduler.add_job('scrape_headlines', headline_all_category, trigger="interval", seconds=60 * 60 * 2)
scheduler.start()