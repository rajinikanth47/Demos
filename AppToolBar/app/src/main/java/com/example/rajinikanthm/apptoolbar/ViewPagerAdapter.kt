package com.example.rajinikanthm.apptoolbar

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

/**
 * Created by rajinikanthm on 12/9/18.
 */
class ViewPagerAdapter(fm:FragmentManager) : FragmentStatePagerAdapter(fm){

    var fm:FragmentManager?=null
    init {
        this.fm = fm
    }

    override fun getItem(position: Int): Fragment {
       return when(position){
           1-> FragmentOne()
           else ->FragmentTwo()
       }
    }

    override fun getCount(): Int {
        return 2
    }
}