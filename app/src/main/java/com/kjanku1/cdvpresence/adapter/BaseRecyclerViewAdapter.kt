package com.kjanku1.cdvpresence.adapter

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView

open class BaseRecyclerViewAdapter<T>: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list: ArrayList<T>? = ArrayList<T>()
    protected lateinit var itemClickListener: AdapterView.OnItemClickListener

    fun addItems(items: ArrayList<T>) {
        this.list?.addAll(items)
        reload()
    }

    fun clear() {
        this.list?.clear()
        reload()
    }

    fun getItem(position: Int): T? {
        return this.list?.get(position)
    }

    fun setOnItemClickListener(onItemClickListener: AdapterView.OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    override fun getItemCount(): Int = list!!.size

    private fun reload() {
        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }
}
