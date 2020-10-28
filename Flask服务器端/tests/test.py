from faker import Faker


def users():
    faker = Faker(['zh-CN', 'en_US', 'es_CA'])
    count = 100
    for i in range(count):
        # print(faker.email())
        # print(faker.city())
        # print(faker.past_date(start_date='-3650d'))
        title = faker.sentence(6)
        author = faker.name()
        time = faker.past_datetime(start_date='-800d', tzinfo=None)
        content = faker.text(max_nb_chars=2000, ext_word_list=None)


users()

#
# from faker.providers import internet
#
# fake = Faker('zh_CN')
# # fake.add_provider(internet)
# for _ in range(20):
#     print(fake.address())
