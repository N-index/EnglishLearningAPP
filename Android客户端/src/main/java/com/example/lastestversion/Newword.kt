package com.example.lastestversion

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lastestversion.data.ArticleData
import com.example.lastestversion.data.NewWordData
import com.example.lastestversion.data.Server
import com.example.myapplication.ArticleAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_newword.*
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class Newword : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newword)

        val data = getSharedPreferences("database", Context.MODE_PRIVATE)
        val storedToken = data.getString("token","")
        val user_id = data.getString("user_id","")
        if (storedToken == ""){
            Toast.makeText(this, "登录后即可查看", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }
        val password = ""
        Toast.makeText(this,"长按将单词从生词本中移除",Toast.LENGTH_LONG).show()
        Thread {
            val server = Server()
            val url = server.host + "user/$user_id/newwords/"
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
                    res = connection.errorStream
                    runOnUiThread {
                        Toast.makeText(this, "认证失败，重新登录", Toast.LENGTH_SHORT).show()
                    }
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                HttpURLConnection.HTTP_FORBIDDEN ->{
//                    res = connection.errorStream
                    runOnUiThread {
                        Toast.makeText(this, "权限禁止", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
//                    res = connection.errorStream
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

//            读取所有生词存入数组
            val jsonArray = jsonObject.getJSONArray("newwords")
            val list = ArrayList<NewWordData>()
            var x = 0
            while (x<jsonArray.length()){
                val jsonObject = jsonArray.getJSONObject(x)
                // 填充Article模型列表
                list.add(
                    NewWordData(
                        jsonObject.getInt("id"),
                        jsonObject.getString("word")
                    )
                )
                x++
            }
            runOnUiThread {
                // 将Article列表传入Adapter进行页面内容的适配
                val adapter = NewwordAdapter(this,list)
                newword_list.adapter = adapter }

        }.start()

//        长按
        newword_list.setOnItemLongClickListener { parent, view, position, id ->
            val element:NewWordData = newword_list.adapter.getItem(position) as NewWordData
            val word_id = element.id

            Thread{
                val server = Server()
                val url = server.host + "user/$user_id/newwords/"
                val connection = URL(url).openConnection() as HttpURLConnection
                val credentials = "$storedToken:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "DELETE"
                // 附加json data
                connection.doOutput = true
                connection.setChunkedStreamingMode(0)
                val word_data = JSONObject()
                word_data.put("id", word_id)
                val outputStream: DataOutputStream = DataOutputStream(connection.outputStream)
                outputStream.write(word_data.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
                val res: InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_CREATED -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "删除成功",Toast.LENGTH_SHORT).show()
                        }
                    }
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



                val url_refresh = server.host + "user/$user_id/newwords/"
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
                            Toast.makeText(this, "认证失败，重新登录", Toast.LENGTH_SHORT).show()
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

//            读取所有生词存入数组
                val jsonArray1 = jsonObject1.getJSONArray("newwords")
                val list1 = ArrayList<NewWordData>()
                var x = 0
                while (x<jsonArray1.length()){
                    val jsonObject = jsonArray1.getJSONObject(x)
                    // 填充Article模型列表
                    list1.add(
                        NewWordData(
                            jsonObject.getInt("id"),
                            jsonObject.getString("word")
                        )
                    )
                    x++
                }
                runOnUiThread {
                    // 将Article列表传入Adapter进行页面内容的适配
                    val adapter = NewwordAdapter(this,list1)
                    newword_list.adapter = adapter
                }

            }.start()



            return@setOnItemLongClickListener false
        }



        //      定位底部导航栏c
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navigation_newword
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
