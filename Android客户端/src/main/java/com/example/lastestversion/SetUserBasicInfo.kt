package com.example.lastestversion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lastestversion.data.NewWordData
import com.example.lastestversion.data.Server
import kotlinx.android.synthetic.main.activity_set_user_basic_info.*
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class SetUserBasicInfo : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_user_basic_info)
        val prefs = getSharedPreferences("database", Context.MODE_PRIVATE)
        welcome.text = "亲爱的" + prefs.getString("user_name","陌生人") + ", 拖动滑块定位与您词汇水平相近的词汇量:"
        val stored_vocabulary = prefs.getInt("vocabulary",3000)
        vocabulary.text = stored_vocabulary.toString()
        seekBar.progress = stored_vocabulary/100

        Thread{
            val server = Server()
            val url = server.host + "vocab-example/" + stored_vocabulary/100
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Connection","close")

            connection.requestMethod = "GET"
            val res = connection.inputStream
            val text:String? = res.use {
                it?.reader().use{ reader -> reader?.readText() }
            }
            connection.disconnect()
            val jsonObject = JSONObject(text)
            val jsonArray = jsonObject.getJSONArray("example")
            val textView_array = arrayOf(textView2,textView3,textView4,textView5,textView6,textView7,textView8,textView9,textView10,textView11)
            runOnUiThread {
                textView_array[0].text = jsonArray[0].toString()
                textView_array[1].text = jsonArray[1].toString()
                textView_array[2].text = jsonArray[2].toString()
                textView_array[3].text = jsonArray[3].toString()
                textView_array[4].text = jsonArray[4].toString()
                textView_array[5].text = jsonArray[5].toString()
                textView_array[6].text = jsonArray[6].toString()
                textView_array[7].text = jsonArray[7].toString()
                textView_array[8].text = jsonArray[8].toString()
                textView_array[9].text = jsonArray[9].toString()
            }
        }.start()



        submit_basic_info_button.setOnClickListener {
            val vocabulary = seekBar.progress * 100
            val editor = getSharedPreferences("database", Context.MODE_PRIVATE).edit()
            editor.apply{
                putInt("vocabulary",vocabulary)
            }.apply()
            Thread{
                val data = getSharedPreferences("database", Context.MODE_PRIVATE)
                val user_id = data.getString("user_id","")
                val server = Server()
                val url = server.host + "user/$user_id/set-vocabulary/$vocabulary"
                val connection = URL(url).openConnection() as HttpURLConnection
                val storedToken = prefs.getString("token","")
                val password = ""
                val credentials = "$storedToken:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "POST"

                val res: InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_CREATED -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "提交词汇量成功", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        runOnUiThread {
                            Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    else -> {
                        res = null
                    }
                }
                val text:String? = res.use {
                    it?.reader().use{ reader -> reader?.readText() }
                }
                runOnUiThread {
                    Toast.makeText(this, "提交完成", Toast.LENGTH_SHORT).show()
                }
                connection.disconnect()
                startActivity(Intent(this,MainActivity::class.java))
            }.start()
        }

        seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                vocabulary.text = (progress * 100).toString()
                //获取示例单词
                Thread{
                    val server = Server()

                    val url = server.host + "vocab-example/$progress"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Connection","close")

                    connection.requestMethod = "GET"
                    val res = connection.inputStream
                    val text:String? = res.use {
                        it?.reader().use{ reader -> reader?.readText() }
                    }
                    connection.disconnect()
                    val jsonObject = JSONObject(text)
                    val jsonArray = jsonObject.getJSONArray("example")
                    val textView_array = arrayOf(textView2,textView3,textView4,textView5,textView6,textView7,textView8,textView9,textView10,textView11)
                    var x = 0
                    println(jsonArray.length())
                    runOnUiThread {
                        textView_array[0].text = jsonArray[0].toString()
                        textView_array[1].text = jsonArray[1].toString()
                        textView_array[2].text = jsonArray[2].toString()
                        textView_array[3].text = jsonArray[3].toString()
                        textView_array[4].text = jsonArray[4].toString()
                        textView_array[5].text = jsonArray[5].toString()
                        textView_array[6].text = jsonArray[6].toString()
                        textView_array[7].text = jsonArray[7].toString()
                        textView_array[8].text = jsonArray[8].toString()
                        textView_array[9].text = jsonArray[9].toString()
                    }
                }.start()

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })





    }
}
