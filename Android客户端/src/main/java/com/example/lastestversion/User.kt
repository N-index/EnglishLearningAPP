package com.example.lastestversion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lastestversion.data.Server
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_user.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class User : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CommitPrefEdits", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val data = getSharedPreferences("database",Context.MODE_PRIVATE)
        val user_name = data.getString("user_name","匿名")
        val user_id = data.getString("user_id","")
        val storedToken = data.getString("token","")

        textView.text = "用户名：$user_name"
        textView2.text = "邮箱：：无"
        textView3.text = "词汇量：0"

        if (storedToken == ""){
            Toast.makeText(this, "请先进行登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val password = ""
        Thread{
            val server = Server()
            val url = server.host + "user/$user_id"
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

            val user_vocabulary = jsonObject.getString("vocabulary")
            val username = jsonObject.getString("username")
            val email = jsonObject.getString("email")
            runOnUiThread {
                textView.text = "用户名：$username"
                textView2.text = "邮箱：$email"
                textView3.text =  "词汇量：$user_vocabulary"
            }

        }.start()

        // 重置词汇量
        reset_vocabulary.setOnClickListener {
            startActivity(Intent(this,SetUserBasicInfo::class.java))
        }
        // 阅读历史
        read_history.setOnClickListener {
            startActivity(Intent(this,ReadHistory::class.java))
        }
        // 收藏夹
        collection_button.setOnClickListener {
            startActivity(Intent(this,NewsCollection::class.java))
        }
        // 注销登录
        log_out.setOnClickListener {
            val editor = getSharedPreferences("database", Context.MODE_PRIVATE).edit()
            editor.apply {
                putString("token","")
            }.apply()
            Toast.makeText(this, "已注销", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }






        //      定位底部导航栏c
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navigation_user
        navView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home -> {
                    startActivity(Intent(applicationContext,MainActivity::class.java))
                    overridePendingTransition(0,0)
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
                    return@setOnNavigationItemSelectedListener true
                }
                else -> {
                    return@setOnNavigationItemSelectedListener true
                }
            }

        }
    }
}
