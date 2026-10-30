package com.hotelbooking.reservation.domain;

import com.hotelbooking.shared.domain.Stay;

public enum ServiceUnit {
    PER_STAY {
        public long quantity(Stay stay) {
            return 1;
        }
    },
    PER_NIGHT {
        public long quantity(Stay stay) {
            return stay.nights();
        }
    },
    PER_PERSON_PER_NIGHT {
        public long quantity(Stay stay) {
            return stay.nights() * stay.people();
        }
    };

    public abstract long quantity(Stay stay);
}
