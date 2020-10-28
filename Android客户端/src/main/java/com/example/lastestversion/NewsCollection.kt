package com.example.lastestversion

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lastestversion.data.ArticleData
import com.example.lastestversion.data.Server
import com.example.myapplication.ArticleAdapter
import kotlinx.android.synthetic.main.activity_news_collection.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class NewsCollection : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_collection)

        val data = getSharedPreferences("database", Context.MODE_PRIVATE)
        val storedToken = data.getString("token","")
        val user_id = data.getString("user_id","")
        if (storedToken == ""){
            Toast.makeText(this, "请先进行登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val password = ""

        Thread {
            val server = Server()
            val url = server.host + "user/$user_id/collections/"
            val connection = URL(url).openConnection() as HttpURLConnection
            val credentials = "$storedToken:$password"
            val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
            connection.setRequestProperty("Authorization", "Basic $auth")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Connection","close")

            connection.requestMethod = "GET"

            val res: InputStream?
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    res = connection.inputStream
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    runOnUiThread {
                        Toast.makeText(this, "认证失败，重新登录", Toast.LENGTH_SHORT).show()
                    }
                    startActivity(Intent(this, LoginActivity::class.java))
                    return@Thread
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    runOnUiThread {
                        Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                else -> {
                    return@Thread
                }
            }
            val text:String? = res.use {
                it?.reader().use{ reader -> reader?.readText() }
            }
            connection.disconnect()
            val jsonObject = JSONObject(text)

//            读取所有新闻存入数组
            val jsonArray = jsonObject.getJSONArray("collections")
            val list = ArrayList<ArticleData>()
            var x = 0
            while (x<jsonArray.length()){
                val jsonObject = jsonArray.getJSONObject(x)
                // 填充Article模型列表
                list.add(
                    ArticleData(
                        jsonObject.getInt("id"),
                        jsonObject.getString("title"),
                        jsonObject.getString("sourceName"),
                        jsonObject.getString("author"),
                        jsonObject.getString("count"),
                        jsonObject.getString("description"),
                        jsonObject.getString("URL"),
                        jsonObject.getString("urlToImage"),
                        jsonObject.getString("publish_time"),
                        jsonObject.getString("count_of_new_word"),
                        jsonObject.getString("category")
                    )
                )
                x++
            }
            runOnUiThread {
                // 将Article列表传入Adapter进行页面内容的适配
                val adapter = ArticleAdapter(this,list)
                collection_list.adapter = adapter
            }

        }.start()


        // 单击从收藏列表进入新闻详细内容界面
        collection_list.setOnItemClickListener { parent, view, position, id ->
            val element:ArticleData = collection_list.adapter.getItem(position) as ArticleData
            val intent = Intent(this, DefaultArticleScrolling::class.java).apply {
                putExtra("url",element.url)
                putExtra("article_id",element.id.toString())
                putExtra("title",element.title)
            }
            startActivity(intent)
        }

        //  长按取消收藏
        collection_list.setOnItemLongClickListener { parent, view, position, id ->
            val element:ArticleData = collection_list.adapter.getItem(position) as ArticleData
            val article_id = element.id
            Toast.makeText(this,"要取消收藏的文章ID：$article_id",Toast.LENGTH_SHORT).show()

            // 取消收藏
            Thread{
                val server = Server()
                val url = server.host + "user/$user_id/collections/$article_id"
                val connection = URL(url).openConnection() as HttpURLConnection
                val credentials = "$storedToken:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "DELETE"

                val res: InputStream?
                when (connection.responseCode) {
                    //   204 status code
                    HttpURLConnection.HTTP_NO_CONTENT -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "取消收藏成功",Toast.LENGTH_SHORT).show()
                        }
                    }
                    HttpURLConnection.HTTP_OK -> {
                        res = connection.inputStream
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        runOnUiThread {
                            Toast.makeText(this, "认证信息过期，重新登录", Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this, LoginActivity::class.java))
                        return@Thread
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        runOnUiThread {
                            Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    else -> {
                        return@Thread
                    }
                }
                val text:String? = res.use {
                    it?.reader().use{ reader -> reader?.readText() }
                }
                connection.disconnect()


//                获取新的阅读历史的列表并刷新
                val url_refresh = server.host + "user/$user_id/collections/"
                val connection1 = URL(url_refresh).openConnection() as HttpURLConnection
                connection1.setRequestProperty("Authorization", "Basic $auth")
                connection1.setRequestProperty("Accept", "application/json")
                connection1.setRequestProperty("Connection","close")

                connection1.requestMethod = "GET"
                val res1: InputStream?
                when (connection1.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        res1 = connection1.inputStream
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        res1 = connection1.errorStream
                        runOnUiThread {
                            Toast.makeText(this, "认证信息过期，重新登录", Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    HttpURLConnection.HTTP_FORBIDDEN ->{
                        runOnUiThread {
                            Toast.makeText(this, "权限禁止", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        runOnUiThread {
                            Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    else -> {
                        return@Thread
                    }
                }
                val text1:String? = res1.use {
                    it?.reader().use{ reader -> reader?.readText() }
                }
                connection1.disconnect()
                val jsonObject1 = JSONObject(text1)

//            读取文章列表存入数组
                val jsonArray1 = jsonObject1.getJSONArray("collections")
                val list1 = ArrayList<ArticleData>()
                var x = 0
                while (x<jsonArray1.length()){
                    val jsonObject = jsonArray1.getJSONObject(x)
                    // 填充Article模型列表
                    list1.add(
                        ArticleData(
                            jsonObject.getInt("id"),
                            jsonObject.getString("title"),
                            jsonObject.getString("sourceName"),
                            jsonObject.getString("author"),
                            jsonObject.getString("count"),
                            jsonObject.getString("description"),
                            jsonObject.getString("URL"),
                            jsonObject.getString("urlToImage"),
                            jsonObject.getString("publish_time"),
                            jsonObject.getString("count_of_new_word"),
                            jsonObject.getString("category")
                        )
                    )
                    x++
                }
                runOnUiThread {
                    // 将Article列表传入Adapter进行页面内容的适配
                    val adapter = ArticleAdapter(this,list1)
                    collection_list.adapter = adapter
                }
            }.start()

            return@setOnItemLongClickListener false
        }

    }
}
