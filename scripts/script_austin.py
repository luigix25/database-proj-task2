import json 
import datetime
import csv

timestamp_format 	= "%Y-%m-%d %H:%M:%S"
city				= "Austin"
city_tag			= "ATX" 
stations 			= {}

with open('austin_bikeshare_stations.csv', newline='') as stations_csv:
	reader = csv.DictReader(stations_csv)
	for row in reader:
		#if row["status"] == "active":
			station = {"latitude": row["latitude"], "longitude":row["longitude"]}
			stations[row["station_id"]]  = station

with open('austin_small.csv', newline='') as csvfile:
	reader = csv.DictReader(csvfile)
	for row in reader:
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		time_delta = datetime.timedelta(minutes=int(row["duration_minutes"]))
		time_start = datetime.datetime.strptime(row["start_time"],timestamp_format)

		time["timestamp_start"] = row["start_time"]
		time["timestamp_end"] 	= datetime.datetime.strftime(time_start + time_delta,timestamp_format)

		start_station_id 	= row["start_station_id"][:-2]
		end_station_id 		= row["end_station_id"][:-2]

		try:
			station_start 	= stations[start_station_id]
			station_end 	= stations[end_station_id]
		except Exception as e:
			continue

		space["station_start"] 	= city_tag +":"+ start_station_id
		space["station_end"] 	= city_tag +":"+ end_station_id

		space["latitude_start"] = station_start["latitude"]
		space["latitude_end"] 	= station_end["latitude"]

		space["longitude_start"] = station_start["longitude"]
		space["longitude_end"] 	 = station_end["longitude"]

		rider["type"]			= row["subscriber_type"]

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"] = rider

		print(json.dumps(normalized_row))
