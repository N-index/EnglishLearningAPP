package com.example.lastestversion
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import com.example.lastestversion.data.Server


@SuppressLint("Registered")
class LoginActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

//        登录获取token
        login_button.setOnClickListener {
            val username = findViewById<EditText>(R.id.emailText).text.toString()
            val password = findViewById<EditText>(R.id.passwordText).text.toString()

            Thread {
                val server = Server()
                val url =  server.host + "tokens/"
                val connection = URL(url).openConnection() as HttpURLConnection
                val credentials = "$username:$password"
                val auth = Base64.getEncoder().encode(credentials.toByteArray()).toString(Charsets.UTF_8)
                connection.setRequestProperty("Authorization", "Basic $auth")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "POST"

                val res: InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        res = connection.inputStream
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        res = connection.errorStream
                        runOnUiThread {
                            Toast.makeText(this, "密码错误，重新输入", Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        res = connection.errorStream
                        runOnUiThread {
                            Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        res = null
                    }
                }
                val text:String? = res.use {
                    it?.reader().use{ reader -> reader?.readText() }
                }
                connection.disconnect()

                val jsonObject = JSONObject(text)

                //  获取token后存入本地
                val token = jsonObject.getString("token")
                val user_id = jsonObject.getInt("user_id")
                val user_name = jsonObject.getString("username")
                val editor = getSharedPreferences("database", Context.MODE_PRIVATE).edit()
                editor.apply {
                    putString("token",token)
                    putString("user_id",user_id.toString())
                    putString("user_name",user_name)
                }.apply()

                startActivity(Intent(this,MainActivity::class.java))
            }.start()
        }

        go_to_register.setOnClickListener {
            startActivity(Intent(this,RegisterActivity::class.java))
        }
    }


}