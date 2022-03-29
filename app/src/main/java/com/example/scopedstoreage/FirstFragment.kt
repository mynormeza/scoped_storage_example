package com.example.scopedstoreage

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.scopedstoreage.databinding.FragmentFirstBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private lateinit var takeVideo: ActivityResultLauncher<Uri>
    private lateinit var getPermission: ActivityResultLauncher<Array<String>>
    var photo: Uri? = null
    var video: Uri? = null
    private var cameraGranted = false
    private var writeStorageGranted = false
    private var minSdk29 = false
    private var pickingVideo = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        getPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->

            val cameraResult = results[Manifest.permission.CAMERA] ?: cameraGranted
            val writeStorageResult = results[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writeStorageGranted
            if (cameraResult && writeStorageResult) {
               openCamera()
            } else {
                val cameraAux = this.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                val writeStorageAux = this.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (!cameraAux && !writeStorageAux) {
                    Toast.makeText(requireContext(),"Please go to your device settings and grant access to your camera and storage for this app", Toast.LENGTH_SHORT).show()
                }
            }
        }

        takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            result?.let { flag ->
                if (flag) {
                    binding.ivSelectedImage.setImageURI(photo)
                }
            }
        }

        takeVideo = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { result ->
            result?.let { flag ->
                if (flag) {
                    val thumb = video?.let { getThumbVideo(requireContext(), it)  }
                    binding.ivSelectedImage.setImageBitmap(thumb)
                }
            }
        }
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTakePic.setOnClickListener {
            pickingVideo = false
            if (cameraGranted && writeStorageGranted) {
                openCamera()
            } else {
                checkPermissionsForUpload()
            }

        }

        binding.btnTakeVideo.setOnClickListener {
            pickingVideo = true
            if (cameraGranted && writeStorageGranted) {
                openCamera()
            } else {
                checkPermissionsForUpload()
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createImageFile(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "test_$timeStamp"

        val imagesCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val newImageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageFileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        return requireActivity().applicationContext.contentResolver.insert(imagesCollection, newImageDetails)
    }

    private fun createVideoFile(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val videoFileName = "test_$timeStamp"
        val videosCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val newVideoDetails = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$videoFileName.mp4")
            put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
        }
        return requireActivity().applicationContext.contentResolver.insert(videosCollection, newVideoDetails)
    }

    private fun getThumbVideo(context: Context, videoUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        var mediaMetadataRetriever: MediaMetadataRetriever? = null
        try {
            mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, videoUri)
            bitmap = mediaMetadataRetriever.getFrameAtTime(
                1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaMetadataRetriever?.release()
        }
        return bitmap
    }

    private fun checkPermissionsForUpload() {
        val permissionsToRequest = mutableListOf<String>()
        cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        writeStorageGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || minSdk29

        if (!writeStorageGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!cameraGranted) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (cameraGranted && writeStorageGranted) {
            openCamera()
        } else {
            getPermission.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun openCamera() {
        if (pickingVideo) {
            createVideoFile()?.let {
                video = it
                takeVideo.launch(it)
            }
        } else {
            createImageFile()?.let {
                photo = it
                takePicture.launch(it)
            }
        }
    }
}