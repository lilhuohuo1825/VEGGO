package com.veggo.app.data.remote.api;

import com.google.gson.annotations.SerializedName;
import com.veggo.app.assets.AssetModels;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CertificateApi {
    @GET("certificates")
    Call<CertificateResponse> getCertificates();

    @GET("certificates/requests/customer/{customerId}/latest")
    Call<CertificateRequestResponse> getLatestRequest(@Path("customerId") String customerId);

    class CertificateResponse {
        public boolean success;
        public List<CertificateDto> data;
    }

    class CertificateRequestResponse {
        public boolean success;
        public CertificateRequestDto data;
    }

    class CertificateRequestDto {
        public String id;
        public String requestId;
        public String userId;
        public String CustomerID;
        public String requestedCer;
        public String requestedCertificateID;
        public String requestedCertificateName;
        public String status;
        public String rejectReason;
        public int totalPoints;
        public int carbonPointSnapshot;
        public String createdAt;
        public String reviewedAt;

        public String getCustomerId() {
            return CustomerID != null && !CustomerID.isEmpty() ? CustomerID : userId;
        }

        public String getRequestedCertificateId() {
            return requestedCertificateID;
        }

        public String getRequestedCertificateName() {
            if (requestedCertificateName != null && !requestedCertificateName.isEmpty()) {
                return requestedCertificateName;
            }
            return requestedCer;
        }

        public int getPointSnapshot() {
            return carbonPointSnapshot > 0 ? carbonPointSnapshot : totalPoints;
        }
    }

    class CertificateDto {
        @SerializedName("CertificateID")
        public String certificateId;
        @SerializedName("CertificateName")
        public String certificateName;
        @SerializedName("RequiredCarbonPoint")
        public int requiredCarbonPoint;
        @SerializedName("CertificateDescription")
        public String certificateDescription;
        @SerializedName("RewardDescription")
        public String rewardDescription;
        @SerializedName("Status")
        public boolean status;

        public AssetModels.Certificate toAssetModel() {
            AssetModels.Certificate certificate = new AssetModels.Certificate();
            certificate.certificateId = certificateId;
            certificate.certificateName = certificateName;
            certificate.requiredCarbonPoint = requiredCarbonPoint;
            certificate.certificateDescription = certificateDescription;
            certificate.rewardDescription = rewardDescription;
            certificate.status = status;
            return certificate;
        }
    }
}
