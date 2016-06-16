package com.arashpayan.chirpdemo;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.arashpayan.chirp.Chirp;
import com.arashpayan.chirp.ChirpBrowser;
import com.arashpayan.chirp.ChirpBrowserListener;
import com.arashpayan.chirp.ChirpPublisher;
import com.arashpayan.chirp.Service;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ChirpBrowserListener {

    public static final String TAG = "ChirpDemo";
    private ChirpBrowser mChirpBrowser;
    private ChirpPublisher mChirpPublisher;
    private ServicesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdapter = new ServicesAdapter();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.services_list);
        if (recyclerView != null) {
            recyclerView.setAdapter(mAdapter);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(llm);
        }
        Chirp.Debug = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAdapter.clear();
        mChirpBrowser = Chirp.browseFor("*").
                listener(mAdapter).
                start(getApplication());

        mChirpPublisher = Chirp.publish("com.arashpayan.chirp.demo").
                start(getApplication());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mChirpBrowser != null) {
            mChirpBrowser.stop();
        }
        if (mChirpPublisher != null) {
            mChirpPublisher.stop();
        }
    }

    @Override
    public void onServiceDiscovered(@NonNull Service service) {
        Log.i(TAG, "onServiceDiscovered: " + service);
    }

    @Override
    public void onServiceUpdated(@NonNull Service service) {
        Log.i(TAG, "onServiceUpdated: " + service);
    }

    @Override
    public void onServiceRemoved(@NonNull Service service) {
        Log.i(TAG, "onServiceRemoved: " + service);
    }

    class ServicesAdapter extends RecyclerView.Adapter<ServiceBindingHolder> implements ChirpBrowserListener {

        private ArrayList<Service> mDiscoveredServices = new ArrayList<>();

        public void clear() {
            int numItems = mDiscoveredServices.size();
            mDiscoveredServices.clear();
            notifyItemRangeRemoved(0, numItems);
        }

        @Override
        public ServiceBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.service, parent, false);
            return new ServiceBindingHolder(v);
        }

        @Override
        public void onBindViewHolder(ServiceBindingHolder holder, int position) {
            Service srvc = mDiscoveredServices.get(position);
            holder.getBinding().setVariable(com.arashpayan.chirpdemo.BR.srvc, srvc);
            holder.getBinding().executePendingBindings();
        }

        @Override
        public int getItemCount() {
            return mDiscoveredServices.size();
        }

        @Override
        public void onServiceDiscovered(@NonNull Service service) {
            mDiscoveredServices.add(service);
            notifyItemInserted(mDiscoveredServices.size()-1);
        }

        @Override
        public void onServiceUpdated(@NonNull Service service) {
            int index = mDiscoveredServices.indexOf(service);
            if (index == -1) {
                // shouldn't happen
                return;
            }
            mDiscoveredServices.set(index, service);
            notifyItemChanged(index);
        }

        @Override
        public void onServiceRemoved(@NonNull Service service) {
            int index = mDiscoveredServices.indexOf(service);
            if (index != -1) {
                mDiscoveredServices.remove(index);
                notifyItemRemoved(index);
            }
        }
    }
}
