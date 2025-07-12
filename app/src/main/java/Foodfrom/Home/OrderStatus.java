package Foodfrom.Home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import Foodfrom.Home.Common.Common;
import Foodfrom.Home.Interface.ItemClickListener;
import Foodfrom.Home.Model.Request;
import Foodfrom.Home.ViewHolder.OrderViewHolder;

public class OrderStatus extends AppCompatActivity {

    public RecyclerView recyclerView;
    public RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;
    FirebaseDatabase database;
    DatabaseReference requests;
    DatabaseReference users;  // Add reference to 'users' node
    String userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        // Firebase init
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
        users = database.getReference("User"); // Reference to 'users' node

        recyclerView = findViewById(R.id.listOrders);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        showPhoneNumberDialog();
    }

    private void showPhoneNumberDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("One More Step!");
        alertDialog.setMessage("Enter your phone number:");

        final EditText edtPhone = new EditText(this);
        edtPhone.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        alertDialog.setView(edtPhone);

        alertDialog.setPositiveButton("OK", (dialog, which) -> {
            userPhone = edtPhone.getText().toString().trim();
            if (userPhone.isEmpty()) {
                Toast.makeText(OrderStatus.this, "Phone number is required!", Toast.LENGTH_SHORT).show();
                showPhoneNumberDialog(); // Show again if empty
            } else {
                validatePhoneNumber(userPhone);  // Validate phone number
            }
        });

        alertDialog.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(OrderStatus.this, "You need to enter a phone number!", Toast.LENGTH_SHORT).show();
        });

        alertDialog.show();
    }

    private void validatePhoneNumber(String phone) {
        // Check if the phone number exists in the users node
        users.orderByChild("phone").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    loadOrders(phone);  // Load orders if phone exists
                } else {
                    Toast.makeText(OrderStatus.this, "Your phone number is not correct!", Toast.LENGTH_SHORT).show();
                    showPhoneNumberDialog();  // Ask for phone number again
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(OrderStatus.this, "Error validating phone number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOrders(String phone) {
        FirebaseRecyclerOptions<Request> options = new FirebaseRecyclerOptions.Builder<Request>()
                .setQuery(requests.orderByChild("phone").equalTo(phone), Request.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder orderViewHolder, int i, @NonNull Request request) {
                orderViewHolder.txtOrderId.setText(adapter.getRef(i).getKey());
                orderViewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(request.getStatus()));
                orderViewHolder.txtOrderAddress.setText(request.getAddress());
                orderViewHolder.txtOrderPhone.setText(request.getPhone());

                orderViewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClik) {
                        // Handle item click
                    }
                });
            }

            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_layout, parent, false);
                return new OrderViewHolder(view);
            }
        };

        // Set adapter
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }
}
