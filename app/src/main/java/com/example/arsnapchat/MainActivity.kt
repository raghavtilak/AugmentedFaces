package com.example.arsnapchat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import java.util.*

class MainActivity : AppCompatActivity() {
    private var modelRenderable: ModelRenderable? = null
    private var texture: Texture? = null
    private var isAdded = false
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val customArFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as CustomArFragment?
        ModelRenderable.builder()
            .setSource(this, R.raw.fox_face)
            .build()
            .thenAccept { renderable ->
                modelRenderable = renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
            .exceptionally {
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
                null
            }

        Texture.builder()
            .setSource(this, R.drawable.fox_face_mesh_texture)
            .build()
            .thenAccept { textureModel -> texture = textureModel }
            .exceptionally {
                Toast.makeText(this, "Cannot load texture", Toast.LENGTH_SHORT).show()
                null
            }

        customArFragment?.arSceneView?.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)
        customArFragment?.arSceneView?.scene?.addOnUpdateListener {
            if (modelRenderable == null || texture == null) return@addOnUpdateListener

            val frame = customArFragment.arSceneView.arFrame ?: return@addOnUpdateListener
            val augmentedFaces = frame.getUpdatedTrackables(AugmentedFace::class.java)

            for (augmentedFace in augmentedFaces) {
                if (isAdded) return@addOnUpdateListener

                val augmentedFaceNode = AugmentedFaceNode(augmentedFace).apply {
                    setParent(customArFragment.arSceneView.scene)
                    setFaceRegionsRenderable(modelRenderable)
                    setFaceMeshTexture(texture)
                }
                faceNodeMap[augmentedFace] = augmentedFaceNode
                isAdded = true

                val iterator = faceNodeMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key.trackingState == TrackingState.STOPPED) {
                        entry.value.setParent(null)
                        iterator.remove()
                    }
                }
            }
        }
    }
}
