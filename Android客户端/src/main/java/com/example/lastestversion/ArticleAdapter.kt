package com.example.myapplication
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.widget.AppCompatTextView
import com.example.lastestversion.R
import com.example.lastestversion.data.ArticleData

// 新闻列表适配器
class ArticleAdapter(val context:Context, private val list: ArrayList<ArticleData>):BaseAdapter() {

    @SuppressLint("ViewHolder", "SetTextI18n", "WrongViewCast")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        // 找到对应的单篇Article卡片
        val view:View = LayoutInflater.from(context).inflate(R.layout.article_row,parent,false)

        val articleTitle = view.findViewById(R.id.article_title) as AppCompatTextView
        val articleSource = view.findViewById(R.id.source) as AppCompatTextView
        val articleDescription = view.findViewById(R.id.description) as AppCompatTextView
        val articlePublishedTime = view.findViewById(R.id.publishedTime) as AppCompatTextView


//        // 获取文章对象、填充内容
//        articleId.text = list[position].id.toString()
        articleTitle.text = list[position].title
        articleSource.text = list[position].source
        val time:String = list[position].publishedTime
        articlePublishedTime.text = time.substring(0,time.length-7)
        articleDescription.text = "Introduce: " +
                list[position].description +
                "\n[文章字数" + list[position].count + "]" +
                "[预计阅读时长" + list[position].count.toInt()/60 + "分钟]"





        return view
    }

    // 获取单篇Article
    override fun getItem(position: Int): Any {
        return list[position]
    }

    // 单篇Article的ID号
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // ArticleList的数量
    override fun getCount(): Int {
        return list.size
    }
}