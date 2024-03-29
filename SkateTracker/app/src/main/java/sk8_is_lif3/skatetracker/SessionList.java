package sk8_is_lif3.skatetracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.ChangeBounds;
import android.support.transition.ChangeTransform;
import android.support.transition.TransitionInflater;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.transition.Transition;
import android.support.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sk8_is_lif3.skatetracker.transitions.SessionNameTransition;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link SessionList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SessionList extends Fragment {

    private static final String TAG = "SessionList: ";
    private RecyclerView sessionRecyclerView;
    private RecyclerView.LayoutManager sessionLayoutManager;
    private RecyclerView trickGridView;
    private GridLayoutManager trickLayoutManager;
    private FirebaseUser user;
    List<String> sessionList;
    private FirestoreRecyclerAdapter<SessionToDisplay, SessionViewHolder> adapter;
    private FirestoreRecyclerAdapter<TrickToDisplay, TrickViewHolder> trickAdapter;
    private boolean isNewUser;

    public SessionList() {
        // Required empty public constructor
    }
    public static SessionList newInstance(String param1, String param2) {
        SessionList fragment = new SessionList();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        sessionRecyclerView = getView().findViewById(R.id.sessionsRecyclerView);
        sessionLayoutManager = new LinearLayoutManager(getContext());
        sessionRecyclerView.setLayoutManager(sessionLayoutManager);
        sessionRecyclerView.setAdapter(adapter);
        sessionRecyclerView.setNestedScrollingEnabled(false);

        trickLayoutManager = new GridLayoutManager(getContext(), 2);
        trickGridView = getView().findViewById(R.id.trickRecyclerView);
        trickGridView.setHasFixedSize(false);
        trickGridView.setLayoutManager(trickLayoutManager);
        trickGridView.setAdapter(trickAdapter);
        trickGridView.addItemDecoration(new SpacesItemDecoration(20));
        trickGridView.setNestedScrollingEnabled(true);




        final FloatingActionButton floatingActionButton = (FloatingActionButton)getView().findViewById(R.id.newSessionFab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CurrentSession.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("session time", 0);
                getActivity().startActivity(intent);
            }
        });


        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.toolbar);
        toolbar.setTitle("My Activity");
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        activity.setSupportActionBar(toolbar);


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionList = new ArrayList<String>();
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        System.out.println("NEW SESSIONS");

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {

            Query query = db.collection("users").document(user.getUid()).collection("sessions").orderBy("date", Query.Direction.DESCENDING).limit(4);
            Query trickQuery = db.collection("users").document(user.getUid()).collection("tricks").whereGreaterThanOrEqualTo("avgRatio", 0.0).orderBy("avgRatio", Query.Direction.DESCENDING).limit(4);

            db.collection("users").document(user.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if(documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null && data.get("newUser") != null && getView() != null) {
                            isNewUser = (boolean) (data.get("newUser"));
                            if (isNewUser) {
                                new MaterialTapTargetPrompt.Builder(getActivity())
                                        .setTarget(getView().findViewById(R.id.newSessionFab))
                                        .setPrimaryText("Start a new session here")
                                        .setSecondaryText("Sessions are a primary way you can track your progress")
                                        .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                                            @Override
                                            public void onHidePrompt(MotionEvent event, boolean tappedTarget) {

                                            }

                                            @Override
                                            public void onHidePromptComplete() {

                                            }
                                        })
                                        .setBackgroundColour(getResources().getColor(R.color.colorAccent))
                                        .show();
                            }
                        }
                    }
                }
            });

            FirestoreRecyclerOptions<SessionToDisplay> options = new FirestoreRecyclerOptions.Builder<SessionToDisplay>()
                    .setQuery(query, SessionToDisplay.class)
                    .build();

            FirestoreRecyclerOptions<TrickToDisplay> trickOptions = new FirestoreRecyclerOptions.Builder<TrickToDisplay>()
                    .setQuery(trickQuery, TrickToDisplay.class)
                    .build();

            trickAdapter = new FirestoreRecyclerAdapter<TrickToDisplay, TrickViewHolder>(trickOptions) {
                @NonNull
                @Override
                public TrickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    // create a new view
                    View v = (View) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.trick_grid_card_layout, parent, false);
                    final TrickViewHolder vh = new TrickViewHolder(v);

                    return vh;
                }

                @Override
                protected void onBindViewHolder(@NonNull final TrickViewHolder holder, final int position, @NonNull final TrickToDisplay model) {
                    //final CardView cardView = holder.itemView.findViewById(R.id.card_view);
                    holder.trickNameView.setText(model.getName());
                    holder.trickNameView.setMaxLines(1);
                    holder.trickNameView.setTransitionName("trickNameTransition" + Integer.toString(position));
                    holder.trickNameView.setTextColor(Color.WHITE);
                    holder.trickPicture.setImageDrawable
                            (
                                    getResources().getDrawable(getResourceID("trick_" + Integer.toString(position+1), "drawable",
                                            getContext()), null)
                            );

                    DecimalFormat df = new DecimalFormat("#.##");
                    df.setRoundingMode(RoundingMode.CEILING);
                    double val = Double.valueOf(df.format(model.getAvgRatio())) * 100;
                    int value = (int) val;
                    holder.trickRatioView.setText(Integer.toString(value) + "%");
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            setSharedElementReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));

                            TrickDetailFragment nextFrag = new TrickDetailFragment(model.getName().toUpperCase().toString(), model.getAvgRatio(), model.getDbID(), model.getSessions());

                            nextFrag.setSharedElementEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move));
                            nextFrag.setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                            nextFrag.setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                            nextFrag.setReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));

                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setReorderingAllowed(true)
                                    .addSharedElement(holder.trickNameView, holder.trickNameView.getTransitionName())
                                    .replace(R.id.fragment, nextFrag,"TrickDetailFragment")
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                }
            };
            trickAdapter.startListening();

            adapter = new FirestoreRecyclerAdapter<SessionToDisplay, SessionViewHolder>(options) {

                @NonNull
                @Override
                public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    // create a new view
                    View v = (View) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.session_card_layout, parent, false);
                    final SessionViewHolder vh = new SessionViewHolder(v);

                    return vh;
                }

                @Override
                protected void onBindViewHolder(@NonNull final SessionViewHolder holder, final int position, @NonNull final SessionToDisplay model) {
                    //final CardView cardView = holder.itemView.findViewById(R.id.card_view);
                    holder.sessionNameView.setText(model.getDate() + " - " + model.getName());
                    holder.sessionNameView.setMaxLines(2);
                    holder.sessionNameView.setTransitionName("sessionNameTransition" + model.getId());
                    holder.sessionNameView.setTextColor(Color.WHITE);

                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            Transition mainTransition = TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade);
                            mainTransition.setDuration(250);
                            mainTransition.setStartDelay(375);

                            Transition textTransScale = new SessionNameTransition();
                            textTransScale.setDuration(375);
                            textTransScale.setInterpolator(new FastOutSlowInInterpolator());

                            Transition textTransMove = new ChangeTransform();
                            textTransMove.setDuration(375);
                            textTransMove.setInterpolator(new FastOutSlowInInterpolator());

                            Transition textTransBounds = new ChangeBounds();
                            textTransBounds.setDuration(375);
                            textTransBounds.setInterpolator(new FastOutSlowInInterpolator());

                            long duration = 375;

                            int screenSize = getView().getResources().getConfiguration().screenLayout &
                                    Configuration.SCREENLAYOUT_SIZE_MASK;

                            switch (screenSize) {
                                case Configuration.SCREENLAYOUT_SIZE_LARGE:
                                    duration = 390;
                                    break;
                                case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                                    duration = 300;
                                    break;
                                case Configuration.SCREENLAYOUT_SIZE_SMALL:
                                    duration = 210;
                                    break;
                                default:
                            }
                            mainTransition.setStartDelay(duration);
                            textTransScale.setDuration(duration);
                            textTransMove.setDuration(duration);
                            textTransBounds.setDuration(duration);

                            TransitionSet tSet = new TransitionSet().addTransition(textTransMove).addTransition(textTransScale).addTransition(textTransBounds);

                            setSharedElementReturnTransition(tSet);
                            //setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));

                            SessionDetailFragment nextFrag = new SessionDetailFragment(model.getDate() + " - " + model.getName(), model.getTotalTimeFormatted(), model.getId(), model.getTricks());

                            nextFrag.setSharedElementEnterTransition(tSet);
                            nextFrag.setEnterTransition(mainTransition);
                            nextFrag.setExitTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));
                            nextFrag.setReturnTransition(TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.fade));

                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setReorderingAllowed(true)
                                    .addSharedElement(holder.sessionNameView, holder.sessionNameView.getTransitionName())
                                    .replace(R.id.fragment, nextFrag,"SessionDetailFragment")
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                }

            };
            adapter.startListening();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_session_list, container, false);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(trickAdapter != null) {
            trickAdapter.startListening();
            trickAdapter.onDataChanged();
        }
        if(adapter != null) {
            adapter.startListening();
            adapter.onDataChanged();
        }

    }

    @Override
    public void onPause(){
        super.onPause();
        if (adapter != null) {
            adapter.stopListening();
        }
        if (trickAdapter != null) {
            trickAdapter.stopListening();
        }
    }

    private class SessionViewHolder extends RecyclerView.ViewHolder {
        private View view;

        // each data item is just a string in this case
        public TextView sessionNameView;
        public View itemView;
        public ImageView removeButton;
        public SessionViewHolder(View v) {
            super(v);
            itemView = v;
            sessionNameView = v.findViewById(R.id.trickName);
        }
    }

    private class TrickViewHolder extends RecyclerView.ViewHolder {
        private View view;

        // each data item is just a string in this case
        public TextView trickNameView, trickRatioView;
        public ImageView trickPicture;
        public View itemView;
        public ImageView removeButton;
        public TrickViewHolder(View v) {
            super(v);
            itemView = v;
            trickPicture = (ImageView)v.findViewById(R.id.imageView);
            trickNameView = v.findViewById(R.id.trickName);
            trickRatioView = v.findViewById(R.id.trickRatio);
        }
    }

    private class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space/2;
            outRect.top = space;

        }
    }
    protected final static int getResourceID
            (final String resName, final String resType, final Context ctx)
    {
        final int ResourceID =
                ctx.getResources().getIdentifier(resName, resType,
                        ctx.getApplicationInfo().packageName);
        if (ResourceID == 0)
        {
            throw new IllegalArgumentException
                    (
                            "No resource string found with name " + resName
                    );
        }
        else
        {
            return ResourceID;
        }
    }

    private void LimitNumberOfFragments(){
        if(getActivity().getSupportFragmentManager().getBackStackEntryCount() > 4){
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

}
