package com.example.lastestversion.data

// 新闻数据模型
data class ArticleData(val id:Int,
                       val title:String,
                       val source:String,
                       val author:String,
                       val count:String,
                       val description:String,
                       val url:String,
                       val urlToImage:String,
                       val publishedTime:String,
                       val count_of_new_word:String,
                       val category:String
)