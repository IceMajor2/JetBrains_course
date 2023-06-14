package platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import platform.dtos.CodeRequestDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Table(name = "codes")
@Entity
@JsonPropertyOrder({"code", "date", "time", "views"})
public class Code {

    private static Long AUTO_INCREMENT_NUM_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonIgnore
    private UUID id;

    @JsonIgnore
    @Nonnull
    private Long numId;

    @Nonnull
    private String code;

    @JsonIgnore
    @Nonnull
    private LocalDateTime date;

    @Nonnull
    private long time;

    @Nonnull
    private long views;

    @Nonnull
    @JsonIgnore
    private boolean restricted;

    @Nonnull
    @JsonIgnore
    private boolean toBeRestricted;

    public Code(CodeRequestDTO codeRequestDTO) {
        this.code = codeRequestDTO.getCode();

        if (codeRequestDTO.getTime() < 0) {
            this.time = 0;
        } else {
            this.time = codeRequestDTO.getTime();
        }

        if (codeRequestDTO.getViews() < 0) {
            this.views = 0;
        } else {
            this.views = codeRequestDTO.getViews();
        }

        this.date = LocalDateTime.now();
        this.setNumId();
        this.restricted = false;
        this.toBeRestricted = determineRestriction(this.time, this.views);
    }

    public Code() {
    }

    public boolean determineRestriction(long time, long views) {
        if (time <= 0 && views <= 0) {
            return false;
        }
        return true;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public long getTime() {
        if (this.time == 0) {
            return this.time;
        }
        updateRestrictedTime();
        return this.time;
    }

    private void updateRestrictedTime() {
        long secondsDiff = ChronoUnit.SECONDS.between(this.date, LocalDateTime.now());
        this.time -= secondsDiff;
        this.time = this.time < 0 ? 0 : this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }

    public void setNumId() {
        this.numId = AUTO_INCREMENT_NUM_ID;
        AUTO_INCREMENT_NUM_ID++;
    }

    public Long getNumId() {
        return numId;
    }

    public void setNumId(Long numId) {
        this.numId = numId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("date")
    public String getDateFormatted() {
        return this.date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String toString() {
        return this.code;
    }
}
