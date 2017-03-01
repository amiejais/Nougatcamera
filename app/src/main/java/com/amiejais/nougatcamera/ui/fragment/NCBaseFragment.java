package com.amiejais.nougatcamera.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Class provides the basic implementation for sub classes.
 */
public abstract class NCBaseFragment extends Fragment {

    // Root view for this fragment
    private View mFragmentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mFragmentView = inflater.inflate(initializeLayoutId(), null);
        return mFragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeViews(savedInstanceState);
        loadContent();
    }

    /**
     * Initialize fragment views.
     */
    protected abstract void initializeViews(Bundle savedInstanceState);

    /**
     * Load initial content of the fragment.
     */
    protected abstract void loadContent();

    /**
     * @return returns layout id of the fragment.
     */
    protected abstract int initializeLayoutId();

    /**
     * @return root view of the fragment.
     */
    protected View getFragmentView() {
        return mFragmentView;
    }
}
