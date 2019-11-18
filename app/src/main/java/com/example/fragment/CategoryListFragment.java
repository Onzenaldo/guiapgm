package com.example.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.softwareonze.guiapgm.MyApplication;
import com.softwareonze.guiapgm.R;
import com.softwareonze.guiapgm.SearchActivity;
import com.example.adapter.CategoryListAdapter;
import com.example.item.ItemPlaceList;
import com.example.util.API;
import com.example.util.Constant;
import com.example.util.JsonUtils;
import com.example.util.OnLoadMoreListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class CategoryListFragment extends Fragment {

    ScrollView mScrollView;
    ProgressBar mProgressBar;
    ArrayList<ItemPlaceList> mCategoryList;
    RecyclerView mCategoryView;
    CategoryListAdapter categoryListAdapter;
    MyApplication MyApp;
    private LinearLayout lyt_not_found;
    String Id, Name;
    int page = 1, totalPage;
    boolean isLoadMore = false, isFirst = true;
    Activity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_category, container, false);
         activity = getActivity();
        if (getArguments() != null) {
            Id = getArguments().getString("Id");
            Name = getArguments().getString("name");
        }
        MyApp = MyApplication.getAppInstance();
        mCategoryList = new ArrayList<>();

        mScrollView = rootView.findViewById(R.id.scrollView);
        mProgressBar = rootView.findViewById(R.id.progressBar);
        mCategoryView = rootView.findViewById(R.id.vertical_courses_list);
        lyt_not_found = rootView.findViewById(R.id.lyt_not_found);

        mCategoryView.setHasFixedSize(true);
        mCategoryView.setLayoutManager(new GridLayoutManager(getActivity(), 1));
        mCategoryView.setFocusable(false);
        mCategoryView.setNestedScrollingEnabled(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API());
        jsObj.addProperty("method_name", "get_place_by_cat_id");
        jsObj.addProperty("cat_id", Id);
        jsObj.addProperty("page", page);
        if (JsonUtils.isNetworkAvailable(requireActivity())) {
            new Home(API.toBase64(jsObj.toString())).execute(Constant.API_URL);
        }

        setHasOptionsMenu(true);
        return rootView;
    }

    @SuppressLint("StaticFieldLeak")
    private class Home extends AsyncTask<String, Void, String> {

        String base64;

        private Home(String base64) {
            this.base64 = base64;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (activity != null && isFirst)
                showProgress(true);
        }

        @Override
        protected String doInBackground(String... params) {
            return JsonUtils.getJSONString(params[0], base64);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (isFirst)
                showProgress(false);
            if (null == result || result.length() == 0) {
                lyt_not_found.setVisibility(View.VISIBLE);
            } else {

                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray(Constant.CATEGORY_ARRAY_NAME);
                    if (jsonArray.length() > 0) {
                        isLoadMore = true;
                        JSONObject objJson;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            objJson = jsonArray.getJSONObject(i);
                            if (objJson.has("status")) {
                                lyt_not_found.setVisibility(View.VISIBLE);
                            } else {
                                ItemPlaceList objItem = new ItemPlaceList();
                                objItem.setPlaceId(objJson.getString(Constant.LISTING_H_ID));
                                objItem.setPlaceCatId(objJson.getString(Constant.LISTING_H_CAT_ID));
                                objItem.setPlaceName(objJson.getString(Constant.LISTING_H_NAME));
                                objItem.setPlaceImage(objJson.getString(Constant.LISTING_H_IMAGE));
                                objItem.setPlaceVideo(objJson.getString(Constant.LISTING_H_VIDEO));
                                objItem.setPlaceDescription(objJson.getString(Constant.LISTING_H_DES));
                                objItem.setPlaceAddress(objJson.getString(Constant.LISTING_H_ADDRESS));
                                objItem.setPlaceEmail(objJson.getString(Constant.LISTING_H_EMAIL));
                                objItem.setPlacePhone(objJson.getString(Constant.LISTING_H_PHONE));
                                objItem.setPlaceWebsite(objJson.getString(Constant.LISTING_H_WEBSITE));
                                objItem.setPlaceLatitude(objJson.getString(Constant.LISTING_H_MAP_LATITUDE));
                                objItem.setPlaceLongitude(objJson.getString(Constant.LISTING_H_MAP_LONGITUDE));
                                objItem.setPlaceRateAvg(objJson.getString(Constant.LISTING_H_RATING_AVG));
                                objItem.setPlaceRateTotal(objJson.getString(Constant.LISTING_H_RATING_TOTAL));

                                mCategoryList.add(objItem);
                            }
                        }
                     }
                    else {
                        isLoadMore=false;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (isFirst) {
                    setResult();
                } else {
                    categoryListAdapter.notifyDataSetChanged();
                    categoryListAdapter.setLoaded();
                }
            }
        }
    }

    private void setResult() {
        if (getActivity() != null) {
            categoryListAdapter = new CategoryListAdapter(getActivity(), mCategoryList, mCategoryView);
            mCategoryView.setAdapter(categoryListAdapter);

            if (categoryListAdapter.getItemCount() == 0) {
                lyt_not_found.setVisibility(View.VISIBLE);
            } else {
                lyt_not_found.setVisibility(View.GONE);
            }

            categoryListAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (isLoadMore) {
                        mCategoryList.add(null);
                        mCategoryView.post(new Runnable() {
                            public void run() {
                                categoryListAdapter.notifyItemInserted(mCategoryList.size() - 1);
                            }
                        });
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mCategoryList.remove(mCategoryList.size() - 1);
                                categoryListAdapter.notifyItemRemoved(mCategoryList.size());
                                 if (JsonUtils.isNetworkAvailable(activity)) {
                                    isFirst = false;
                                    page = page + 1;
                                    JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API());
                                    jsObj.addProperty("method_name", "get_place_by_cat_id");
                                    jsObj.addProperty("cat_id", Id);
                                    jsObj.addProperty("page", page);
                                    if (JsonUtils.isNetworkAvailable(activity)) {
                                        new Home(API.toBase64(jsObj.toString())).execute(Constant.API_URL);
                                    }

                                }

                            }
                        }, 1200);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.no_more), Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
            mCategoryView.setVisibility(View.GONE);
            lyt_not_found.setVisibility(View.GONE);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mCategoryView.setVisibility(View.VISIBLE);
        }
    }
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);

        final SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();

        final MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub
                if (!hasFocus) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO Auto-generated method stub


                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                intent.putExtra("search", query);
                startActivity(intent);
                searchView.clearFocus();
                return false;
            }
        });

    }

}
