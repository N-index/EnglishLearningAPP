package com.example.lastestversion

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lastestversion.data.Server
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_recommend.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class Recommend : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommend)
        val data = getSharedPreferences("database", Context.MODE_PRIVATE)
        val storedToken = data.getString("token","")
        if (storedToken == ""){
            Toast.makeText(this, "请先进行登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val password = ""

        playSoundUK.setOnClickListener {
            val soundData = getSharedPreferences("database", Context.MODE_PRIVATE)
            val ukSoundUrl = soundData.getString("uk","")
            Toast.makeText(this,ukSoundUrl,Toast.LENGTH_SHORT).show()
            val mediaPlayer: MediaPlayer? = null
            mediaPlayer?.setDataSource(ukSoundUrl)
            mediaPlayer?.start()
        }
        playSoundUS.setOnClickListener {
            val soundData = getSharedPreferences("database", Context.MODE_PRIVATE)

            val usSoundUrl = soundData.getString("usa","")
            Toast.makeText(this,usSoundUrl,Toast.LENGTH_SHORT).show()
            val mediaPlayer: MediaPlayer? = null
            mediaPlayer?.setDataSource(usSoundUrl)
            mediaPlayer?.start()
        }


        query_button.setOnClickListener {
            val query_text = query_text.text.toString()
            Thread{
                val server = Server()
                val url = server.host + "translation/"
                val connection = URL(url).openConnection() as HttpURLConnection
                val credentials = "$storedToken:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")


                connection.doOutput = true
                connection.setChunkedStreamingMode(0)

                val wordlist = JSONArray()
                wordlist.put(query_text)

                val query_data = JSONObject()
                query_data.put("word_list",wordlist)

                val outputStream: DataOutputStream = DataOutputStream(connection.outputStream)
                outputStream.write(query_data.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()

                connection.requestMethod = "POST"
                val res: InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "OK:200", Toast.LENGTH_SHORT).show()
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


                val jsonArray = JSONArray(text)
                val jsonObject = jsonArray[0] as JSONObject

//              翻译
                val translation = jsonObject.get("translation").toString()
//              发音文件、音标内容、音标类型
                val pho:JSONArray = jsonObject.getJSONArray("phonetic")
                var pho_res = "发音：\n"
                //存储发音文件
                val editor = getSharedPreferences("database", Context.MODE_PRIVATE).edit()
                for (i in 0 until pho.length()){
                    val item = pho.getJSONObject(i)
                    val pho_filename = item.get("filename").toString()

                    val pho_text = item.get("text").toString()
                    val pho_type = item.get("type").toString()
                    editor.apply{
                        putString(pho_type, pho_filename)
                    }.apply()
                    pho_res = "$pho_res $pho_type:[$pho_text]\n"
                }
//              不同词性的不同释义
                val usual:JSONArray = jsonObject.getJSONArray("usual")
                var pos_res = "详细释义：\n"
                for (i in 0 until usual.length()) {
                    val item = usual.getJSONObject(i)
                    val type = item.get("pos").toString()
                    val value_text = item.get("values") as JSONArray
                    val value = value_text[0].toString()
                    pos_res = "$pos_res $type $value\n"
                }
//              展示结果
                val query_res = "翻译：$translation\n\n$pho_res\n$pos_res"
                runOnUiThread {
                    query_result_view.text  = query_res
                }

            }.start()
        }

        //      定位底部导航栏c
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navigation_recommend
        navView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home -> {
                    startActivity(Intent(applicationContext,MainActivity::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_recommend ->{
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

