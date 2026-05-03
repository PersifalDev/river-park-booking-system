DELETE FROM service_item_photo;

INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/room_service/room-service.png', NOW(), NOW() FROM service_item WHERE type = 1;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/contactless_check_in/contactless_check_in.png', NOW(), NOW() FROM service_item WHERE type = 2;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/sauna/sauna.png', NOW(), NOW() FROM service_item WHERE type = 3;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/parking/parking.png', NOW(), NOW() FROM service_item WHERE type = 4;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/playroom/playroom.png', NOW(), NOW() FROM service_item WHERE type = 5;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/transfer/transfer.png', NOW(), NOW() FROM service_item WHERE type = 6;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/gym/gym.png', NOW(), NOW() FROM service_item WHERE type = 7;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/advert/advert.png', NOW(), NOW() FROM service_item WHERE type = 8;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/hotel_dry_cleaning/hotel_dry_cleaning.png', NOW(), NOW() FROM service_item WHERE type = 9;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/location_for_photo/location_for_photo.png', NOW(), NOW() FROM service_item WHERE type = 10;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/beauty_salon/beauty_salon.png', NOW(), NOW() FROM service_item WHERE type = 11;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/communications_and_internet/communications_and_internet.png', NOW(), NOW() FROM service_item WHERE type = 12;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/park_with_trampoline/park_with_trampoline.png', NOW(), NOW() FROM service_item WHERE type = 13;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/bike_rental/bike_rental.png', NOW(), NOW() FROM service_item WHERE type = 14;
INSERT INTO service_item_photo (service_item_id, path, created_at, updated_at)
SELECT id, 'images/services/boat_tours/boat_tours.png', NOW(), NOW() FROM service_item WHERE type = 15;
