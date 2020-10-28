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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.article_list
import kotlinx.android.synthetic.main.activity_read_history.*
import kotlinx.android.synthetic.main.activity_recommend.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val data = getSharedPreferences("database", Context.MODE_PRIVATE)
        val storedToken = data.getString("token","")
        if (storedToken == ""){
            Toast.makeText(this, "请先进行登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val password = ""

        Thread {
            val server = Server()
            val url = server.host + "articles/"
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
                        Toast.makeText(this, "认证已过期，请重新登录", Toast.LENGTH_SHORT).show()
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
            val jsonArray = jsonObject.getJSONArray("articles")
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
                article_list.adapter = adapter
            }
        }.start()

        article_list.setOnItemClickListener { parent, view, position, id ->
            val element:ArticleData = article_list.adapter.getItem(position) as ArticleData
            val intent = Intent(this, DefaultArticleScrolling::class.java).apply {
                putExtra("url",element.url)
                putExtra("article_id",element.id.toString())
                putExtra("title",element.title)

            }
            startActivity(intent)
        }


        //      定位底部导航栏c
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navigation_home
        navView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home -> {
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_recommend ->{
                    startActivity(Intent(applicationContext,Recommend::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_newword ->{
                    startActivity(Intent(applicationContext,Newword::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_user -> {
                    startActivity(Intent(applicationContext,User::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                else -> {
                    return@setOnNavigationItemSelectedListener true
                }
            }

        }
    }
}
