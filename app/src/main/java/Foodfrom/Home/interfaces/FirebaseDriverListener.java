package Foodfrom.Home.interfaces;


import Foodfrom.Home.Model.Driver;


public interface FirebaseDriverListener {

    void onDriverAdded(Driver driver);

    void onDriverRemoved(Driver driver);

    void onDriverUpdated(Driver driver);

}
