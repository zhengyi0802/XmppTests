package tk.munditv.mundichat.ui.controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import tk.munditv.mundichat.R;
import tk.munditv.mundichat.viewmodel.ControllerViewModel;

public class ControllerFragment extends Fragment {

    private ControllerViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(ControllerViewModel.class);
        View root = inflater.inflate(R.layout.fragment_controller, container, false);
        final TextView textView = root.findViewById(R.id.command_string);
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}
