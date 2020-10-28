package com.example.lastestversion

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
import kotlinx.android.synthetic.main.activity_single_article.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class SingleArticleScrollingActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_single_article)

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
        val user_id = data.getInt("user_id",-1)
//        val article_id = intent.getStringExtra("article_id")

        article_title.text = title

       //点击收藏
        fab.setOnClickListener {
            Thread{
                val prefs = getSharedPreferences("database",Context.MODE_PRIVATE)
                val article_id = prefs.getInt("article_id",-1)
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
                article_content.text = article_detail.content
            }

            val inputstream:InputStream = URL(article_detail.urlToImage).openStream()
            val bitmap = BitmapFactory.decodeStream(inputstream)
            runOnUiThread {

                imageView.setImageBitmap(bitmap)
            }



            val editor = getSharedPreferences("database",Context.MODE_PRIVATE).edit()
            editor.apply {
                putInt("article_id",article_detail.id)
            }.apply()

        }.start()


    }
}
