package com.landtanin.teacherattendancemonitoring.fragment;

import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.landtanin.teacherattendancemonitoring.R;
import com.landtanin.teacherattendancemonitoring.adapter.StudentListAdapter;
import com.landtanin.teacherattendancemonitoring.dao.LecturerModuleCollectionDao;
import com.landtanin.teacherattendancemonitoring.dao.StudentAttendanceDao;
import com.landtanin.teacherattendancemonitoring.databinding.FragmentStudentBinding;
import com.landtanin.teacherattendancemonitoring.manager.HttpManager;
import com.landtanin.teacherattendancemonitoring.manager.http.ApiService;
import com.landtanin.teacherattendancemonitoring.util.Utils;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by nuuneoi on 11/16/2014.
 */
public class FragmentStudent extends Fragment {

    private StudentListAdapter mStudentListAdapter;
    FragmentStudentBinding b;
    private Realm realm;
    private String moduleId;
    ProgressDialog dialog;

    public FragmentStudent() {
        super();
    }

    public static FragmentStudent newInstance() {
        FragmentStudent fragment = new FragmentStudent();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);

        if (savedInstanceState != null)
            onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_student, container, false);
        View rootView = b.getRoot();
        initInstances(rootView, savedInstanceState);
        return rootView;
    }

    @SuppressWarnings("UnusedParameters")
    private void init(Bundle savedInstanceState) {
        // Init Fragment level's variable(s) here
    }

    @SuppressWarnings("UnusedParameters")
    private void initInstances(View rootView, Bundle savedInstanceState) {
        // Init 'View' instance(s) with rootView.findViewById here
        // Note: State of variable initialized here could not be saved
        //       in onSavedInstanceState

        moduleId = getActivity().getIntent().getStringExtra("module_id");
        dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Please Wait...");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        getStudent(moduleId);



    }

    private void getStudent(String module_id){

        ApiService apiService = HttpManager.getInstance().create(ApiService.class);
//        apiService.loadStudentModule(Authorization,Content_Type,developer.getMemberID(),TopicId)
        apiService.loadModuleStudent(module_id)
                .asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Utils.getInstance().defaultSubscribeScheduler())
                .unsubscribeOn(Utils.getInstance().defaultSubscribeScheduler())
                .subscribe(new Action1<LecturerModuleCollectionDao>() {
                    @Override
                    public void call(LecturerModuleCollectionDao response) {

                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
//                        realm.deleteAll(); // clear the current data before load new data
                        realm.delete(StudentAttendanceDao.class); // delete only data of a specific class
                        realm.copyToRealmOrUpdate(response.getStudents());
                        realm.commitTransaction();
                        dialog.dismiss();

                        realm = Realm.getDefaultInstance();
                        RealmResults<StudentAttendanceDao> studentAttendanceDao = realm.where(StudentAttendanceDao.class).findAll();

                        Log.w("FragmentStudent", String.valueOf(studentAttendanceDao.size()));

                        if (studentAttendanceDao.size()!=0) {

                            StaggeredGridLayoutManager rvLayoutManager = new StaggeredGridLayoutManager(1, 1);
                            b.rvStudent.setLayoutManager(rvLayoutManager);
                            mStudentListAdapter = new StudentListAdapter(getContext(), studentAttendanceDao, true);

                            b.rvStudent.setAdapter(mStudentListAdapter);
                            b.rvStudent.setHasFixedSize(true);
                            b.noStudent.setVisibility(View.GONE);

                        } else {

                            b.rvStudent.setVisibility(View.GONE);
                            b.noStudent.setText("You are free today");
                            b.noStudent.setVisibility(View.VISIBLE);

                        }

                        Log.d("getStudent", "call success");

                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        dialog.dismiss();
                        Utils.getInstance().onHoneyToast("STUDENT "+throwable.getLocalizedMessage());

                    }
                });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save Instance (Fragment level's variables) State here
    }

    @SuppressWarnings("UnusedParameters")
    private void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore Instance (Fragment level's variables) State here
    }


}
