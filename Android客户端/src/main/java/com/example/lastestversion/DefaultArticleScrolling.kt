package com.example.lastestversion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.lastestversion.data.ArticleDetailData
import com.example.lastestversion.data.Server
import kotlinx.android.synthetic.main.activity_default_article_scrolling.*
import kotlinx.android.synthetic.main.activity_default_article_scrolling.fab
import kotlinx.android.synthetic.main.content_default_article_scrolling.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class DefaultArticleScrolling : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_article_scrolling)
//        setSupportActionBar(toolbar)

        fab.setImageResource(R.drawable.ic_stars_24px)

        val data = getSharedPreferences("database", Context.MODE_PRIVATE)
        val storedToken = data.getString("token","")
        if (storedToken == ""){
            Toast.makeText(this, "请先进行登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val password = ""
        val apiurl = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val article_id = intent.getStringExtra("article_id")
        val user_id = data.getString("user_id","")

//        Toast.makeText(this,"$apiurl title:$title ArticleID:$article_id",Toast.LENGTH_SHORT).show()


        default_title_content.text = "$title\n\n内容加载中..."

        //点击收藏
        fab.setOnClickListener {
            Thread{
                val server = Server()
                val url = server.host + "user/$user_id/collections/$article_id"
                val connection = URL(url).openConnection() as HttpURLConnection
                val credentials = "$storedToken:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "POST"
                val res: InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "OK:200", Toast.LENGTH_SHORT).show()
                        }
                    }
                    HttpURLConnection.HTTP_CREATED -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "已收藏文章:201", Toast.LENGTH_SHORT).show()
                            fab.setColorFilter(Color.WHITE)
                        }
                    }
                    HttpURLConnection.HTTP_NO_CONTENT -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "无返回内容:204", Toast.LENGTH_SHORT).show()
                        }
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
            }.start()
        }


        //获取文章内容
        Thread{
            val server = Server()

            val url = server.basehost + apiurl
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

            val article_detail = ArticleDetailData(
                jsonObject.getInt("id"),
                jsonObject.getString("title"),
                jsonObject.getString("count"),
                jsonObject.getString("content"),
                jsonObject.getString("author"),
                jsonObject.getString("URL"),
                jsonObject.getString("sourceURL"),
                jsonObject.getString("urlToImage"),
                jsonObject.getString("publish_time"),
                jsonObject.getString("category"),
                jsonObject.getString("number_of_unknown")

            )
            runOnUiThread {
                default_title_content.text = "$title\n生词率：" +
                        ((article_detail.number_of_unknown.toFloat()/article_detail.count.toFloat()) * 100).toString().substring(0,2)  +
                        "%\n\n" +
                        article_detail.content
            }

            val inputstream:InputStream = URL(article_detail.urlToImage).openStream()
            val bitmap = BitmapFactory.decodeStream(inputstream)
            runOnUiThread {
                default_article_image.setImageBitmap(bitmap)
            }

            val editor = getSharedPreferences("database",Context.MODE_PRIVATE).edit()
            editor.apply {
                putInt("article_id",article_detail.id)
            }.apply()

        }.start()


    }
}
