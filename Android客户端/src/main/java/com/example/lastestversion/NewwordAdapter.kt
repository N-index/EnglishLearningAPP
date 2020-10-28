package com.example.lastestversion

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.widget.AppCompatTextView
import com.example.lastestversion.data.NewWordData


// 新闻列表适配器
class NewwordAdapter(val context: Context, private val list: ArrayList<NewWordData>): BaseAdapter() {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        // 找到对应的单个生词卡片
        val view: View = LayoutInflater.from(context).inflate(R.layout.newword_row,parent,false)

        //在布局结构中找到要填充的元素
        val id = view.findViewById(R.id.newword_id) as AppCompatTextView
        val word = view.findViewById(R.id.word) as AppCompatTextView

//
//        // 获取文章对象、填充内容
//        id.text = list[position].id.toString()
        id.text = (position+1).toString()
        word.text = list[position].word



        return view
    }

    // 获取单个生词
    override fun getItem(position: Int): Any {
        return list[position]
    }

    // 单个生词的ID号
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // 生词的数量
    override fun getCount(): Int {
        return list.size
    }
}