package com.example.lastestversion
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lastestversion.data.Server
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

const val EXTRA_MESSAGE = "com.example.lastestversion.MESSAGE"

@SuppressLint("Registered")
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailText).text.toString()
            val username = findViewById<EditText>(R.id.username).text.toString()
            val password = findViewById<EditText>(R.id.passwordText).text.toString()
            val password2 = findViewById<EditText>(R.id.passwordText2).text.toString()
            if(password!=password2){
                Toast.makeText(this, "两次密码不一致",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Thread {
//                发送请求注册用户
                val server = Server()
                val url = server.host + "user/"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection","close")

                connection.requestMethod = "POST"
                // 附加json data
                connection.doOutput = true
                connection.setChunkedStreamingMode(0)
                val register_data = JSONObject()
                register_data.put("email",email)
                register_data.put("username",username)
                register_data.put("password",password)
                val outputStream: DataOutputStream = DataOutputStream(connection.outputStream)
                outputStream.write(register_data.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()

                val res:InputStream?
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_CREATED -> {
                        res = connection.inputStream
                        runOnUiThread {
                            Toast.makeText(this, "注册成功",Toast.LENGTH_SHORT).show()
                        }
                    }
                    HttpURLConnection.HTTP_OK -> {
                        res = connection.inputStream
                    }
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        runOnUiThread {
                            Toast.makeText(this, "用户名已注册", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        res = connection.errorStream
                        runOnUiThread {
                            Toast.makeText(this, "认证失败，重新登录", Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
//                        res = connection.errorStream
                        runOnUiThread {
                            Toast.makeText(this, "服务器错误", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                    else -> {
                        res = null
                    }
                }

                connection.connect()
                val text:String? = res.use {
                    it?.reader().use{ reader -> reader?.readText() }
                }
                connection.disconnect()

                val jsonObject = JSONObject(text)
                val token = jsonObject.getString("token")
                val expiration = jsonObject.getString("expiration")
                val user_id = jsonObject.getInt("user_id")
                val user_name = jsonObject.getString("username")
                val editor = getSharedPreferences("database", Context.MODE_PRIVATE).edit()
                editor.apply {
                    putString("token",token)
                    putString("expiration",expiration)
                    putString("user_id",user_id.toString())
                    putString("user_name",user_name)
                }.apply()

                startActivity(Intent(this,SetUserBasicInfo::class.java))
            }.start()
        }

        go_to_login.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
        }
    }
}