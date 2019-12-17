import json
import datetime
import csv

timestamp_format 	= "%Y-%m-%d %H:%M:%S"
city				= "New York"
city_tag			= "NYC"

with open('../datasets/newyork_small.csv', newline='') as csvfile:
	reader = csv.DictReader(csvfile)
	for row in reader:
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		time["timestamp_start"] = row["pickup_datetime"]
		time["timestamp_end"] 	= row["dropoff_datetime"]

		#start_station_id 	= row["start_station_id"][:-2]
		#end_station_id 		= row["end_station_id"][:-2]

		#space["station_start"] 	= city_tag +":"+ start_station_id
		#space["station_end"] 	= city_tag +":"+ end_station_id

		space["latitude_start"] = row["pickup_latitude"]
		space["latitude_end"] 	= row["dropoff_latitude"]

		space["longitude_start"] = row["pickup_longitude"]
		space["longitude_end"] 	 = row["dropoff_longitude"]

		if row["gender_id"] == '1':
			rider["gender"] = 'M'
		elif row["gender_id"] == '2':
			rider["gender"] = 'F'

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"] = rider

		print(json.dumps(normalized_row))
