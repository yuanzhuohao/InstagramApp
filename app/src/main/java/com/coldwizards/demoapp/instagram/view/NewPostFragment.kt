package com.coldwizards.demoapp.instagram.view

import android.app.Activity
import android.app.ProgressDialog.show
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.coldwizards.coollibrary.divider.GridPlacingDecoration
import com.coldwizards.coollibrary.view.GalleryActivity
import com.coldwizards.demoapp.R
import com.coldwizards.demoapp.camerademo.CameraAppActivity
import com.coldwizards.demoapp.camerademo.CameraFragment
import com.coldwizards.demoapp.instagram.PostPictureAdapter
import com.coldwizards.demoapp.instagram.viewmodel.NewPostViewModel
import com.coldwizards.demoapp.utils.FileUtils
import com.coldwizards.demoapp.utils.launchActivity
import com.coldwizards.demoapp.utils.showCenterToast
import kotlinx.android.synthetic.main.fragment_new_post.*
import java.io.File


class NewPostFragment : BaseFragment() {

    private val REQUEST_CAPTURE_IMAGE = 1
    private val REQUEST_PICK_IMAGE = 0
    private val REQUEST_PHOTO_VIEWER = 2

    private val postViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(NewPostViewModel::class.java)
    }

    private val mAdapter = PostPictureAdapter()
    private var mImageFile = File("")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_new_post, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.new_post_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.post -> {
                if (postViewModel.newPost(
                        content.text.toString(),
                        mAdapter.getData()
                    ) == 1
                ) {
                    showCenterToast("请登录")
                } else {
                    showLoading("发布中...")
                    Handler().postDelayed({

                        dismissLoading()

                        view!!.findNavController().navigate(R.id.action_newPostFragment_to_postListFragment)
                    }, 1500)
                }
            }
            android.R.id.home -> {
                view!!.findNavController().popBackStack()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val images = arguments?.getStringArrayList("data")
        images?.let {
            mAdapter.setData(it)
        }

        setToolbar()
        setRecyclerView()

        (activity as InsActivity).userViewModel.mUserLiveData.observe(this, Observer {
            postViewModel.mUser = it
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                REQUEST_CAPTURE_IMAGE -> {
                    val images = arrayListOf<String>(
                        data?.getStringExtra(CameraFragment.FILE_PATH_PARAM)?:""
                    )

                    mAdapter.addData(images[0])
                }
                REQUEST_PICK_IMAGE -> {
                    val images = data?.getStringArrayListExtra(GalleryActivity.SELECTED_IMAGE)
                    images?.let {
                        mAdapter.addData(images)
                    }
                }
                REQUEST_PHOTO_VIEWER -> {
                    val images = data?.getStringArrayListExtra(PhotoViewerActivity.IMAGES)
                    images?.let {
                        mAdapter.setData(images)
                    }
                }
            }
        }
    }

    private fun setToolbar() {
        getToolbar().apply {
            title = "New Post"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
        }
    }

    private fun setRecyclerView() {
        // 点击每个Item的回调
        mAdapter.setItemClickListener {position, arrayList, b ->
            if (b) {
                showSelectDialog()
            } else {
                Intent(context!!, PhotoViewerActivity::class.java).apply {
                    putExtra("images", arrayList)
                    putExtra("position", position)
                    startActivityForResult(this, REQUEST_PHOTO_VIEWER)
                }
            }
        }

        pictures.apply {
            layoutManager = GridLayoutManager(context!!, 3)
            adapter = mAdapter
            addItemDecoration(
                GridPlacingDecoration(
                    3, 20, false
                )
            )
        }
    }

    private fun showSelectDialog() {
        AlertDialog.Builder(context!!).apply {
            setTitle("选择图片来源")
            setItems(arrayOf("拍照", "从相册中选择")) { listener, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> {
                        val intent = Intent(context!!, GalleryActivity::class.java)
                        intent.putExtra(GalleryActivity.MAX_NUM, 9 - mAdapter.getData().size)
                        intent.putExtra(GalleryActivity.SELECTABLE, true)
                        startActivityForResult(intent, REQUEST_PICK_IMAGE)
                    }
                }
            }
            create()
            show()
        }
    }

    /**
     * 打开相机拍照
     */
    private fun takePhoto() {
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//
//        mImageFile = FileUtils.createImageFile(
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath +
//                    "/" + "Instagram"
//        )
//
//        if (mImageFile != null) {
//            val uri = FileProvider.getUriForFile(
//                context!!,
//                "com.coldwizards.demoapp.provider",
//                mImageFile
//            )
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
//            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE)
//        }

        val intent = Intent(context!!, CameraAppActivity::class.java).apply {
            putExtra(CameraFragment.REQUEST_TAKE_PHOTO_PARAM, true)
        }
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE)
    }
}
