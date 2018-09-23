package fliqdemo.mdaq.com.fliqdemoapp.activities

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import fliqdemo.mdaq.com.fliqdemoapp.R
import fliqdemo.mdaq.com.fliqdemoapp.adapters.VoucherListAdapter
import fliqdemo.mdaq.com.fliqdemoapp.database.entity.Voucher
import fliqdemo.mdaq.com.fliqdemoapp.listeners.OnSwipeTouchListener
import fliqdemo.mdaq.com.fliqdemoapp.listeners.RecyclerItemClickListener
import fliqdemo.mdaq.com.fliqdemoapp.utils.Logger
import fliqdemo.mdaq.com.fliqdemoapp.utils.ThemeUtil
import fliqdemo.mdaq.com.fliqdemoapp.viewmodels.ThemesViewModel
import fliqdemo.mdaq.com.fliqdemoapp.viewmodels.VoucherViewModel
import kotlinx.android.synthetic.main.vochers_home_activity.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class VouchersHomeActivity:AbstractBaseActivity(){

    private var voucherList:MutableList<Voucher>?=null
    private var voucherViewModel:VoucherViewModel?=null
    private var adapter:VoucherListAdapter?=null
    private var themeViewModel:ThemesViewModel?=null

    private var defaultVoucher:Voucher?=null


    companion object {
        const val ACTIVITY_REQ_FOR_REWARDS= 101
        const val ACTIVITY_REQ_FOR_SCANNING = 102
        const val DEFAULT_POINTS = 5000
    }

    override fun getContentView(): Int {
        return R.layout.vochers_home_activity
    }

    override fun onViewReady(savedInstanceState: Bundle?, intent: Intent) {
        super.onViewReady(savedInstanceState, intent)
        voucherViewModel = getVoucherModelInstance()
        themeViewModel = getThemeModelInstance()

        voucher_home_root.setOnTouchListener(object : OnSwipeTouchListener(applicationContext) {
            override fun onSwipeRight(){
               onBackPressed()
            }
        })

        applyTheme()

        val listView =  findViewById<RecyclerView>(R.id.voucher_list_view)
        listView.layoutManager = LinearLayoutManager(this)
        adapter = VoucherListAdapter(this)
        listView.adapter = adapter

        listView.addOnItemTouchListener(RecyclerItemClickListener(this, recyclerView = listView!!, mListener = object : RecyclerItemClickListener.OnItemClickListener {
            override fun onLongItemClick(view: View?, position: Int) {
                Logger.printW("TAG", "Long press not implemented.")
            }
            override fun onItemClick(view: View, position: Int) {
                if(position != -1) {
                    val i = Intent(applicationContext,RewardsActivity::class.java)
                    val voucher = voucherList!![position]
                    i.putExtra("voucher",voucher)
                    startActivityForResult(i,ACTIVITY_REQ_FOR_REWARDS)
                }
            }
        }))
        val fButton=findViewById<FloatingActionButton>(R.id.cameraForReadReceipt)
        fButton.setOnClickListener{
           val i =  Intent(this,VoucherCameraActivity::class.java)
            startActivityForResult(i,ACTIVITY_REQ_FOR_SCANNING)
        }
    }

    private fun applyTheme(){
        val root = findViewById<ConstraintLayout>(R.id.voucher_home_root)
        val logo = findViewById<ImageView>(R.id.voucherLogo)
        val cameraReceipt = findViewById<ImageView>(R.id.cameraForReadReceipt)
        ThemeUtil.setVoucherHomeTheme(this,root,logo,cameraReceipt)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != 0){
            when (requestCode) {
                ACTIVITY_REQ_FOR_REWARDS -> finish()
                ACTIVITY_REQ_FOR_SCANNING -> topUpPointsForFirstVoucher()
            }
        }
    }

    private fun topUpPointsForFirstVoucher(){
        launch {
            val res = async {
                val v = defaultVoucher
                v!!.points = v.points!! + DEFAULT_POINTS
                voucherViewModel!!.updateVoucher(v)
            }.await()
            if(res != 0){
                loadVoucherFromDB()
            }
        }
    }

    override fun onResumeActivity() {
        super.onResumeActivity()
        loadVoucherFromDB()
    }

    private fun loadVoucherFromDB(){
        try {
            if(voucherList != null && voucherList!!.isNotEmpty())
                voucherList!!.clear()

            launch {
                var res = async {
                   val theme =  themeViewModel!!.getBaseTheme()
                    voucherList = when {
                        theme.sequenceNumber == 0 -> voucherViewModel!!.getAllVouchers()
                        else -> voucherViewModel!!.getVoucherBasedOnTheme(theme!!.title)
                    }
                }.await()
                launch(UI) {
                    if(voucherList!= null) {
                        defaultVoucher = voucherList!![0]
                        adapter!!.setVoucherList(voucherList)
                    }else{
                        Toast.makeText(applicationContext,"Problem while loading vouchers!!,please try again",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }
}