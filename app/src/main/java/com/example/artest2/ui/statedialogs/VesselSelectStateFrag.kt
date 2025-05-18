import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.example.artest2.databinding.FragmentSselSelectStateBinding
import com.example.artest2.R

// VesselSelectStateFrag.kt


class VesselSelectStateFrag : DialogFragment() {
    // Define what kind of internal button was clicked in the dialog
    enum class DialogButtonType {
        AUXILIARY_ACTION_1, // For your myNewButton
        // Add more if you have other specific buttons inside the dialog's view
    }

    interface VesselDialogInteractionsListener {
        fun onVesselConfirmed(vesselName: String)
        fun onVesselSelectionCancelled()
        // Modified to pass the type of button and any relevant data
        fun onDialogInternalButtonClicked(buttonType: DialogButtonType, data: Bundle?)
    }
    private var _binding: FragmentSselSelectStateBinding? = null


    private var interactionListener: VesselDialogInteractionsListener? = null
    // ... (binding, etc.)

    fun setInteractionListener(listener: VesselDialogInteractionsListener) {
        this.interactionListener = listener
    }
    private val binding get() = _binding!! // Non-null accessor

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // In VesselSelectStateFrag class
            // ... (spinner setup if any) ...

            // This is your myNewButton (ID vessel_select_button)
            binding.vesselSelectButton.setOnClickListener {
                Log.d(
                    "VesselSelectStateFrag",
                    "vessel_select_button (myNewButton) clicked in dialog."
                )
                // Prepare any data this specific button click might need to send
                val eventData = Bundle().apply {
                    putString("button_id", "vessel_select_button")
                    putString("message_hint", "Action from Vessel Dialog's special button")
                }
                // Tell the listener WHAT was clicked and pass data
                interactionListener?.onDialogInternalButtonClicked(
                    DialogButtonType.AUXILIARY_ACTION_1,
                    eventData
                )
            }

        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setTitle("Vessel Selection State")
            .setPositiveButton("Confirm") { _, _ -> /* interactionListener?.onVesselConfirmed(...) */ }
            .setNegativeButton("Cancel") { _, _ -> /* interactionListener?.onVesselSelectionCancelled() */ }
            .create()
    }
    // In VesselSelectionFragment.kt (when it's ready to return a result)
    // This assumes VesselSelectionFragment was launched in a way that expects a result
    // via an Activity intermediary or a specific contract.
    private fun sendResultBack(requestId: String, dataMap: HashMap<String, Any>?) {
        val resultBundle = Bundle().apply {
            putString("requestId", requestId)
            putSerializable("dataMap", dataMap)
        }
        // If launched by an Activity for result:
        // requireActivity().setResult(Activity.RESULT_OK, Intent().putExtras(resultBundle))
        // requireActivity().finish()

        // If using Jetpack Navigation Component & Fragment Result API:
        // (This is the more common way for Fragment-to-Fragment)
        parentFragmentManager.setFragmentResult("VesselSelectionKey_from_DashboardFragment", resultBundle)
        //findNavController().popBackStack() // Go back to DashboardFragment
    }
}

