package org.mtransit.parser.ca_london_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.londontransit.ca/open-data/
// http://www.londontransit.ca/gtfsfeed/google_transit.zip
public class LondonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-london-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LondonTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating London Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating London Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return String.valueOf(Integer.parseInt(gRoute.getRouteShortName())); // remove leading 0
		}
		return super.getRouteShortName(gRoute);
	}

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(^route [\\d]+[\\s]?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(EMPTY);
		if (StringUtils.isEmpty(routeLongName)) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "Kipps Lane to Pond Mills Rd/King Edward";
			case 2: return "Natural Science – Trafalgar Heights / Bonaventure";
			case 3: return "Downtown – Argyle";
			case 4: return "Fanshawe College – White Oaks Mall";
			case 5: return "Byron – Argyle Mall";
			case 6: return "University Hospital – Parkwood Institute";
			case 7: return "Westmount Mall – Argyle Mall";
			case 9: return "Downtown – Whitehills";
			case 10: return "Natural Science to Barker at Huron";
			case 12: return "Downtown – Wharncliffe & Wonderland";
			case 13: return "White Oaks Mall – Masonville Place";
			case 15: return "Huron Heights – Westmount Mall";
			case 16: return "Masonville Mall – Pond Mills";
			case 17: return "Argyle Mall to Byron/Riverbend";
			case 19: return "Downtown – Stoney Creek";
			case 20: return "Fanshawe College to Beaverbrook";
			case 24: return "Talbot Village – Summerside";
			case 25: return "Fanshawe College to Masonville Place";
			case 27: return "Fanshawe College to Capulet";
			case 28: return "White Oaks Mall – Lambeth";
			case 30: return "White Oaks Mall to Cheese Factory Rd";
			case 31: return "Alumni Hall – Hyde Park Power Centre";
			case 33: return "Alumni Hall to Proudfoot";
			case 34: return "Masonville Place to Alumni Hall/Natural Science";
			case 35: return "Argyle Mall – Trafalgar Heights";
			case 36: return "Fanshawe College to London Airport";
			case 37: return "Argyle Mall to Neptune Crescent";
			case 90: return "Masonville Mall - White Oaks Mall";
			case 91: return "Fanshawe College - Oxford & Wonderland";
			case 93: return "White Oaks Mall to Masonville Place";
			case 94: return "Express: Natural Science – Argyle Mall";
			case 102: return "Downtown – Natural Science";
			case 104: return "Ridout @ Grand – Fanshawe College";
			case 106: return "Downtown to Natural Science";
			// @formatter:on
			}
			throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute.toStringPlus());
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "009F60"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final int LTC_EASTBOUND = 1;
	private static final int LTC_NORTHBOUND = 2;
	private static final int LTC_SOUTHBOUND = 3;
	private static final int LTC_WESTBOUND = 4;

	private static final int LTC_CW = 10;
	private static final int LTC_CCW = 11;

	private static final int LTC_HURON_AND_BARKER = 101;
	private static final int LTC_WESTERN = 102;

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(31L, new RouteTripSpec(31L, // SPLITTED BECAUSE NO HEAD-SIGN PROVIDED
				LTC_EASTBOUND, MTrip.HEADSIGN_TYPE_STRING, "Alumni Hall", //
				LTC_WESTBOUND, MTrip.HEADSIGN_TYPE_STRING, "Hyde Park Power Centre") //
				.addTripSort(LTC_EASTBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("SEAGHYPK"), Stops.getALL_STOPS().get("1653"), // Seagull at Hyde Park WB
								Stops.getALL_STOPS().get("ALUMHAL2"), Stops.getALL_STOPS().get("142") // Alumni Hall WB
						)) //
				.addTripSort(LTC_WESTBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("ALUMHAL2"), Stops.getALL_STOPS().get("142"), // Alumni Hall WB
								Stops.getALL_STOPS().get("SEAGHYPK"), Stops.getALL_STOPS().get("1653") // Seagull at Hyde Park WB
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(34L, new RouteTripSpec(34L, // SPLITTED BECAUSE SAME HEAD-SIGN before "via"
				LTC_CW, MTrip.HEADSIGN_TYPE_STRING, "CW via Natural Science", // "Masonville Pl", //
				LTC_CCW, MTrip.HEADSIGN_TYPE_STRING, "CCW via Alumni Hall") // "Masonville Pl") //
				.addTripSort(LTC_CW, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("MASOSTO2"), Stops.getALL_STOPS().get("1141"), // Masonville Place Stop #2
								Stops.getALL_STOPS().get("STACFAN1"), Stops.getALL_STOPS().get("2879"), // Stackhouse at Fanshawe SB
								Stops.getALL_STOPS().get("MASOSTO2"), Stops.getALL_STOPS().get("1141") // Masonville Place Stop #2
						)) //
				.addTripSort(LTC_CCW, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("MASOSTO3"), Stops.getALL_STOPS().get("1142"), // Masonville Place Stop #3
								Stops.getALL_STOPS().get("ALUMHAL1"), Stops.getALL_STOPS().get("141"), // Alumni Hall EB
								Stops.getALL_STOPS().get("MASOSTO3"), Stops.getALL_STOPS().get("1142") // Masonville Place Stop #3
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(91L, new RouteTripSpec(91L, // SPLITTED BECAUSE NO HEAD-SIGN PROVIDED
				LTC_EASTBOUND, MTrip.HEADSIGN_TYPE_STRING, "Fanshawe College", //
				LTC_WESTBOUND, MTrip.HEADSIGN_TYPE_STRING, "Oxford & Wonderland") //
				.addTripSort(LTC_EASTBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("CAPUOXFO"), Stops.getALL_STOPS().get("2390"), // Capulet Lane at Oxford SB
								Stops.getALL_STOPS().get("FANCOLL1"), Stops.getALL_STOPS().get("2331") // Fanshawe College Stop 1
						)) //
				.addTripSort(LTC_WESTBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("FANCOLL1"), Stops.getALL_STOPS().get("2331"), // Fanshawe College Stop 1
								Stops.getALL_STOPS().get("CAPUOXFO"), Stops.getALL_STOPS().get("2390") // Capulet Lane at Oxford SB
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(106L, new RouteTripSpec(106L, //
				LTC_NORTHBOUND, MTrip.HEADSIGN_TYPE_STRING, "Natural Science", //
				LTC_SOUTHBOUND, MTrip.HEADSIGN_TYPE_STRING, "Downtown") //
				.addTripSort(LTC_NORTHBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("RICHQUEE"), Stops.getALL_STOPS().get("1501"),// Richmond at Queens NB - #1501
								Stops.getALL_STOPS().get("RICHQUEE"), Stops.getALL_STOPS().get("1501"), // Richmond at Queens NB - #1501
								Stops.getALL_STOPS().get("NATSCI"), Stops.getALL_STOPS().get("1222") // Natural Science - #1222
						)) //
				.addTripSort(LTC_SOUTHBOUND, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("NATSCI"), Stops.getALL_STOPS().get("1222"), // Natural Science - #1222
								Stops.getALL_STOPS().get("DUNDRIDO"), Stops.getALL_STOPS().get("600"), // Dundas at Ridout EB - #600
								Stops.getALL_STOPS().get("RICHQUEE"), Stops.getALL_STOPS().get("1501"),// Richmond at Queens NB - #1501
								Stops.getALL_STOPS().get("RICHQUEE"), Stops.getALL_STOPS().get("1501") // Richmond at Queens NB - #1501
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String SLASH = " / ";

	private static final String UWO = "UWO";
	private static final String INDUSTRIAL_SHORT = "Ind";
	//
	private static final String MASONVILLE = "Masonville";
	private static final String HOSPITAL_SHORT = "Hosp";

	@SuppressWarnings("RedundantCollectionOperation")
	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault();
		final int directionId = gTrip.getDirectionIdOrDefault();
		if (mRoute.getId() == 1L) {
			if (directionId == 1) { // Kipps Lane - NORTH
				if (Arrays.asList( //
						EMPTY, //
						"Kipps and Adelaide", //
						"1B King edward & Thompson Only", //
						"King edward & Thompson Only", //
						"1A Pond Mills & Thompson Only", //
						"Pond Mills & Thompson Only", //
						"1 Kipps Lane via Wellington & Dundas", //
						"1A Kipps Lane via Wellington & Dundas", //
						"1B Kipps Lane via Wellington & Dundas", //
						"Kipps Lane via Wellington & Dundas" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Pond Mills - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"Shelbourne and Deveron", //
						"Adelaide and Huron Only", //
						"To Adelaide and Huron Only", //
						"1 to Adelaide & Huron Only", //
						"1 Wellington & Dundas Only", //
						"Wellington & Dundas Only", //
						"1B King Edward via Wellington & Dundas", //
						"2B King Edward via Wellington & Dundas", // WTF!
						"King Edward via Wellington & Dundas", //
						"1A King Edward via Wellington & Dundas", //
						"1A Pond Mills Road via Wellington & Dundas", //
						"Pond Mills Road via Wellington & Dundas", //
						"1A Pond Mills via Wellington & Dundas", //
						"Pond Mills via Wellington & Dundas" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 2L) {
			if (directionId == 0) { // Argyle Mall / Bonaventure - EAST
				if (Arrays.asList( //
						EMPTY, //
						"To Dundas and Highbury Only", //
						"2 to Dundas and Highbury Only", //
						"2 to Dundas & Highbury Only", //
						"2 Dundas & Highbury Only", //
						"Dundas & Highbury Only", //
						"2A", //
						"2A to Trafalgar & Hudson", //
						"2A Trafalgar Heights Only", //
						"2A to Trafalgar Heights", //
						"2A Trafalgar and Hudson", //
						"Trafalgar Heights Only", //
						"2A Trafalgar Heigths via Argyle Mall", //
						"2B Bonaventure via Dundas", //
						"Bonaventure via Dundas", //
						"to Argyle Mall Only", //
						"to Dundas and Highbury Only", //
						"2A Argyle Mall via Hale & Trafalgar", //
						"Argyle Mall via Hale & Trafalgar" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Natural Science - WEST
				if (Arrays.asList( //
						EMPTY, //
						"2B Argyle Mall Only", //
						"Argyle Mall Only", //
						"2A to Hale and Brydges Only", //
						"2A Hale and Brydges Only", //
						"2 to Natural Science via Dundas", //
						"2 Natural Science via Dundas", //
						"2A", //
						"2A Natural Science via Dundas", //
						"2B Natural Science via Dundas", //
						"Natural Science via Dundas" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 3L) {
			if (directionId == 1) { // Argyle Mall - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Agyle Mall", //
						"3B Argyle Mall via Hamilton", //
						"Argyle Mall via Hamilton", //
						"3A Argyle Mall via Fairmont", //
						"Argyle Mall via Fairmont" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Downtown - WEST
				if (Arrays.asList( //
						EMPTY, //
						"3A Highbury & Hamilton Only", //
						"3B Highbury & Hamilton Only", //
						"Highbury & Hamilton Only", //
						"3B Downtown via Hamilton", //
						"3A Downtown via Hamilton", //
						"Downtown via Hamilton" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 4L) {
			if (directionId == 1) { // Fanshawe College - NORTH
				if (Arrays.asList( //
						EMPTY, //
						"Fanshawe College", //
						"4B Southdale & Ernest Only", //
						"4 Southdale & Ernest Only", //
						"Southdale & Ernest Only", //
						"4A Fanshawe College via Downtown", //
						"4B Fanshawe College via Downtown", //
						"4 to Fanshawe College", //
						"4 Fanshawe College via Downtown", //
						"Fanshawe College via Downtown" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"4A White Oaks Mall via Downtown", //
						"4B White Oaks Mall via Downtown", //
						"White Oaks Mall via Downtown" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 5L) {
			if (directionId == 1) { // Downtown - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Argyle Mall", //
						"5A Argyle Mall via Springbank", //
						"5B Argyle Mall via Gardenwood", //
						"5 Springbank & Wonderland Only", //
						"Springbank & Wonderland Only", //
						"5B Downtown via Gardenwood",//
						"Downtown via Gardenwood", //
						"5A Downtown Only", //
						"5A Downtown via Springbank", //
						"5B Downtown via Springbank", //
						"Downtown via Springbank" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Byron - WEST
				if (Arrays.asList( //
						EMPTY, //
						"5A Byron via Sprinbank", //
						"5B Byron via Gardenwood", //
						"Byron via Gardenwood", //
						"5A Byron via Springbank", //
						"5 Byron via Springbank", //
						"Byron via Springbank" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 6L) {
			if (directionId == 0) { // Natural Science - NORTH
				if (Arrays.asList( //
						"Baseline & Fairview Only", //
						"Natural Science via Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Parkwood - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"Parkwood Hospital", //
						"Richmond & Dundas Only", //
						"Parkwood Hospital via Richmond", //
						"Parkwood Hosptial via Richmond", //
						"Parkwood via Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 7L) {
			if (directionId == 0) { // Argyle Mall - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Argyle Mall", //
						"Argyle Mall via York" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Downtown - WEST
				if (Arrays.asList( //
						"Brydges & Highbury Only", //
						"Downtown via York" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 9L) {
			if (directionId == 1) { // Downtown - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Wonderland and Blackacres Only", //
						"9C Sarnia & Wonderland Only", //
						"Sarnia & Wonderland Only", //
						"9A Downtown via Sarnia", //
						"9B Downtown via Sarnia", //
						"Downtown via Sarnia", //
						"9C Natural Science via Sarnia", //
						"Natural Science via Sarnia" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Whitehills - WEST
				if (Arrays.asList( //
						EMPTY, //
						"9A Whitehills via Limberlost", //
						"9C Whitehills via Limberlost", //
						"Whitehills via Limberlost", //
						"9B Whitehills via Aldersbrook", //
						"Whitehills via Aldersbrook" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 10L) {
			if (directionId == 0) { // Masonville - WESTERN
				if (Arrays.asList( //
						"White Oaks Mall Only via Highbury", //
						"Highbury/Brydges Only via Highbury", //
						"Natural Science via White Oaks Mall", //
						"Masonville via White Oaks Mall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTERN);
					return;
				}
			} else if (directionId == 1) { // Barker/Huron - HURON_AND_BARKER
				if (Arrays.asList( //
						"Barker/Huron via White Oaks Mall", //
						"Barker/Huron via Highbury", //
						"Barker & Huron via Highbury", //
						"Highbury & Trafalgar Only via White Oaks Mall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_HURON_AND_BARKER);
					return;
				}
			}
		} else if (mRoute.getId() == 11L) {
			if (directionId == 1) { // Downtown - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Downtown Via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Westmount Mall - WEST
				if (Arrays.asList( //
						EMPTY, //
						"Westmount Mall via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 12L) {
			if (directionId == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						"Commissioners & Wharncliffe Only", //
						"Downtown via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Wharncliffe & Wonderland - SOUTH
				if (Arrays.asList( //
						"Wharncliffe & Wonderland Only", //
						"Wharncliffe & Wonderland via Whancliffe", //
						"Wharncliffe & Wonderland via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 13L) {
			if (directionId == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						EMPTY, //
						"13 Masonville Mall", //
						"13 Masonville Mall via Wellington and Richmond", //
						"13 to Masonville Mall", //
						"13 Masonville Mall Only", //
						"Masonville Mall Only", //
						"13A Masonville Mall via Westminster Park", //
						"Masonville Mall via Westminster Park", //
						"13 Masonville Mall via Wellington & Richmond", //
						"Masonville Mall via Wellington & Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"White Oaks Mall", //
						"13 A White Oaks Mall via Westminster Park", //
						"13 to White Oaks Mall", //
						"13A White Oaks Mall via Westminster Park", //
						"White Oaks Mall via Westminster Park", //
						"13 White Oaks Mall via Richmond & Wellington", //
						"White Oaks Mall via Richmond & Wellington" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 14L) {
			if (directionId == 0) { // Huron & Barker - NORTH
				if (Arrays.asList( //
						"Highbury & Brydges Only", //
						"Huron & Barker via Highbury" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"Highbury & Brydges Only", //
						"White Oaks Mall via Highbury" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 15L) {
			if (directionId == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						EMPTY, //
						"15 Oakville and Huron", //
						"15A Oakville and Huron via Downtown", //
						"15B Oakville and Huron via Downtown", //
						"15B Cranbrook and Commissioners Only", //
						"15A Dalhousie and Brixham Only", //
						"15A to Dalhousie and Brixham Only", //
						"15A to Dalhousie and Brixham", //
						"15 B To Cranbrook", //
						"15B to Cranbrook and Commissioners Only", //
						"15B to Cranbrook and Commissioners only", //
						"15A- Downtown via Old South", //
						"15A Downtown via Old South", //
						"Downtown via Old South", //
						"15B Downtown via Cranbrook", //
						"Downtown via Cranbrook" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Westmount Mall - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"15 Highbury and Huron Only", //
						"Westmount  Only", //
						"15A Westmount Mall via Old South", //
						"15 B Westmount Mall via Old South", //
						"15B Westmount Mall via Old South", //
						"Westmount Mall via Old South" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 16L) {
			if (directionId == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"16 Commissioners & Highbury Only", //
						"Commissioners & Highbury Only", //
						"16A Commissioners at Deveron Only", //
						"16 Commissioners at Deveron Only", //
						"Commissioners at Deveron Only", //
						"16 Summerside via Pond Mills", //
						"Summerside via Pond Mills", //
						"16 Masonville Mall via Adelaide", //
						"16B Masvonille Mall via Adelaide", //
						"16B Masonville via Adelaide", //
						"Masonville via Adelaide", //
						"Masvonille Mall via Adelaide", // WTF!
						"16A Masonville Mall via Adelaide", //
						"16B Masonville Mall via Adelaide", //
						"Masonville Mall via Adelaide" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Summerside - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"Commissioners and Deverion via Adelaide", //
						"Deveron at Commissioners Only", //
						"Masonville Mall via Adelaide", //
						"16 Adelaide & King Only", //
						"Adelaide & King Only", //
						"16A Pond Mills via Adelaide", //
						"Pond Mills via Adelaide", //
						"To Pond Mills via Adelaide", //
						"16 Pond Mills & Summerside via Adelaide", //
						"Pond Mills & Summerside via Adelaide", //
						"16B Summerside via Adelaide", //
						"Summerside via Adelaide", //
						"16B Summerside", //
						"Summerside" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 17L) {
			if (directionId == 0) { // Argyle Mall - EAST
				if (Arrays.asList( //
						EMPTY, //
						"17A Oxford & Wonderland Only", //
						"17A Fanshawe College Only", //
						"Fanshawe College Only", //
						"Oxford & Wonderland Only", //
						"17A Boler & Commissioners Only", //
						"Boler & Commissioners Only", //
						"17A Argyle Mall via Oxford", //
						"17B Argyle Mall via Oxford", //
						"Argyle Mall via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Byron / Riverbend - WEST
				if (Arrays.asList( //
						EMPTY, //
						"17 Fanshawe College Only", //
						"17B Riverbend via Oxford", //
						"Riverbend via Oxford", //
						"17 A Byron via Oxford", //
						"17A Byron via Oxford", //
						"17B Byron via Oxford", //
						"Byron via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 19L) {
			if (directionId == 1) { // Downtown - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Fanshawe and North Centre Only", //
						"YMCA Only", //
						"Wellington & Dundas Only", //
						"King at Clarence Only via Hyde Park", //
						"Hyde Park Power Centre via Hyde Park", //
						"Downtown via Hyde Park" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Hyde Pk Power Ctr - WEST
				if (Arrays.asList( //
						EMPTY, //
						"Fanshawe and Hastings Only vis Hyde Park", //
						"Downtown via Hyde Park", //
						"Stoney Creek via Hyde Park", //
						"Hyde Park Power Centre via Hyde Park" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 20L) {
			if (directionId == 0) { // Fanshawe College - EAST
				if (Arrays.asList( //
						"Beaverbrook & Wonderland Only", //
						"Richmond & Dundas Only", //
						"Downtown Only", //
						"Fanshawe College via Downtown" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Beaverbrook - WEST
				if (Arrays.asList( //
						"Beaverbrook via Downtown", //
						"Beaverbrook via Downtow" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 21L) {
			if (directionId == 0) { // Huron Hts - NORTH
				if (Collections.singletonList( //
						"Huron Heights via Cheapside" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Downtown - SOUTH
				if (Arrays.asList( //
						"Huron & Highbury Only", //
						"Downtown via Cheapside" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 24L) {
			if (directionId == 1) { // Victoria Hosp - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Summerside", //
						"Westmount Mall Only", //
						"Summerside via Commissioners", //
						"Commissioners at Highbury Only via Commissioners", //
						"Victoria Hospital via Baseline" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Talbot Vlg - WEST
				if (Arrays.asList( //
						EMPTY, //
						"Westmount Mall Only", //
						"Commissioners at Meadowlily Only", //
						"Talbot Village via Commissioners", //
						"Talbot Village via Baseline" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 25L) {
			if (directionId == 0) { // Masonville Mall - NORTH
				if (Collections.singletonList( //
						"Masonville Mall via Grenfell" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Fanshawe College - SOUTH
				if (Collections.singletonList( //
						"Fanshawe College via Grenfell" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 26L) {
			if (directionId == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						"Jalna & Bradley Only", //
						"Downtown via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // White Oaks Mall - SOUTH
				if (Collections.singletonList( //
						"White Oaks Mall via Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 27L) {
			if (directionId == 1) { // Fanshawe College - EAST
				if (Arrays.asList( //
						"Fanshawe College via Western University", //
						"Fanshawe College via Huron" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Kipps Lane - WEST
				if (Arrays.asList( //
						"Capulet Lane via Western University", //
						"Kipps Lane via Huron" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 28L) {
			if (directionId == 1) { // Westmount Mall - NORTH
				if (Arrays.asList( //
						EMPTY, //
						"Exeter and Wonderland Only", //
						"White Oaks Mall via Exeter", //
						"Wonderland & Wharncliffe only", //
						"Westmount Mall via Wonderland" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Lambeth - SOUTH
				if (Arrays.asList( //
						"Lambeth via Exeter", //
						"Lambeth via Wonderland" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 29L) {
			if (directionId == 0) { // Natural Science - NORTH
				if (Collections.singletonList( //
						"Natural Science via Sarnia" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Capulet Lane - SOUTH
				if (Arrays.asList( //
						"Natural Sciences via Sarnia", //
						"Capulet Lane via Sarnia" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 30L) {
			if (directionId == 0) { // Cheese Factory Road - EAST
				if (Collections.singletonList( //
						"Cheese Factory Road via Newbold" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // White Oaks Mall - WEST
				if (Collections.singletonList( //
						"White Oaks Mall via Wilton Grove" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 31L) {
			if (directionId == 1) { // Alumni Hall - EAST
				if (Arrays.asList( //
						EMPTY, //
						"Alumni Hall via Limberlost" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Hyde Park Power Centre - WEST
				if (Arrays.asList( //
						EMPTY, //
						"Hyde Park Power Centre via Limberlost" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (directionId == 0) { // Huron & Highbury - EAST
				if (Collections.singletonList( //
						"Huron & Highbury via Windermere" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Alumni Hall - WEST
				if (Arrays.asList( //
						"Adelaide & Huron Only", //
						"Alumni Hall via Windermere" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (directionId == 1) { // Alumni Hall - EAST
				if (Arrays.asList( //
						"Proudfoot & Oxford Only", //
						"Alumni Hall via Platts Lane" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Proudfoot - WEST
				if (Collections.singletonList( //
						"Proudfoot via Platts Lane" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 34L) {
			if (directionId == 0) { // Masonville - CLOCKWISE
				if (Arrays.asList( //
						"Garage via Fanshawe Park Rd", //
						"Masonville via Natural Science" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_CW);
					return;
				}
			} else if (directionId == 1) { // Alumni Hall - COUNTER CLOCKWISE
				if (Arrays.asList( //
						"Masonville to Alumni Hall Only", //
						"Masonville via Alumni Hall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_CCW);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (directionId == 1) { // Argyle Mall - NORTH
				if (Arrays.asList( //
						"Royal and Castle Only", //
						"Argyle Mall via Marconi" // <>
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Trafalgar Heights - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"Argyle Mall via Marconi", // <>
						"Trafalgar Heights", //
						"Trafalgar Heights via Marconi" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (directionId == 0) { // London Airport - EAST
				if (Collections.singletonList( //
						"London Airport via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Fanshawe College - WEST
				if (Arrays.asList( //
						"Fanshawe College Only", //
						"Fanshawe College via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (directionId == 1) { // Argyle Mall - NORTH
				if (Arrays.asList( //
						"Sovereign & Trafalgar Only", //
						"Argyle Mall via Sovereign" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Neptune Crescent - SOUTH
				if (Collections.singletonList( //
						"Neptune Crescent via Sovereign" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 38L) {
			if (directionId == 0) { // Stoney Creek - NORTH
				if (Collections.singletonList( //
						"Stoney Creek via Sunningdale" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Masonville Mall - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"Masonville Mall via Sunningdale" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 39L) {
			if (directionId == 1) { // Masonville Mall - EAST
				if (Collections.singletonList( //
						"Masonville Mall via Fanshawe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 0) { // Hyde Park Power Centre - WEST
				if (Collections.singletonList( //
						"Hyde Park Power Centre via Fanshawe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (directionId == 0) { // Northridge - EAST
				if (Arrays.asList( //
						"Northridge & Grenfell via Fanshawe", //
						"Northridge via Fanshawe Pk Rd", //
						"Northridge via Fanshawe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Masonville - WEST
				if (Arrays.asList( //
						"Fanshawe & Adelaide Only", //
						"Masonville via Northridge & Grenfell", //
						"Masonville via Fanshawe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 90L) {
			if (directionId == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"Richmond & Dundas Only", //
						"Express to Masonville Mall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"", //
						"Express to White Oaks Mall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 91L) {
			if (directionId == 0) { // Fanshawe College - EAST
				if (Collections.singletonList( //
						"Express to Fanshawe College" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Oxford & Wonderland - WEST
				if (Arrays.asList( //
						"Express to Oxford and Wonderland", //
						"Express to Oxford & Wonderland" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 92L) {
			if (directionId == 0) { // Masonville Mall - NORTH
				if (Collections.singletonList( //
						"Express to Masonville Mall" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Victoria Hosp - SOUTH
				if (Arrays.asList( //
						"Express to Victoria Hosptial", //
						"Express to Victoria Hospital" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 93L) {
			if (directionId == 0) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"Jalna at Bradley Only", //
						"Masonville Mall Via Wharncliffe/Western" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"Masonville Mall via Western/Wharnclifffe", // <>
						"White Oaks Mall via Western/Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 94L) {
			if (directionId == 0) { // Argyle Mall - EAST
				if (Collections.singletonList( //
						"94 Express to Argyle Mall via Dundas" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_EASTBOUND);
					return;
				}
			} else if (directionId == 1) { // Natural Science - SOUTH
				if (Collections.singletonList( //
						"94 Express to Natural Science via Dundas" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 102L) {
			if (directionId == 1) { // Natural Science - NORTH
				if (Collections.singletonList( //
						"Natural Science via Western/Wharncliffe" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 0) { // Downtown - SOUTH
				if (Arrays.asList( //
						EMPTY, //
						"King and Clarence Only", //
						"Downtown via Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 104L) {
			if (directionId == 0) { // Fanshawe College - NORTH
				if (Collections.singletonList( //
						"Fanshawe College via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Ridout & Grand - SOUTH
				if (Arrays.asList( //
						"Richmond & Dundas Only", //
						"Ridout & Grand via Oxford" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 106L) {
			if (directionId == 0) { // Natural Science - NORTH
				if (Collections.singletonList( //
						"Natural Science via Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_NORTHBOUND);
					return;
				}
			} else if (directionId == 1) { // Downtown - SOUTH
				if (Collections.singletonList( //
						"Downtown via Richmond" //
				).contains(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), LTC_SOUTHBOUND);
					return;
				}
			}
		}
		throw new MTLog.Fatal("%s: Unexpected trip %s!", mRoute.getId(), gTrip.toStringPlus());
	}

	private static final String A = "A";
	private static final String A_ = "A ";
	private static final String B_ = "B ";
	private static final String C_ = "C ";

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"Shelbourne & Deveron", //
					"Adelaide & Huron", //
					"Wellington & Dundas", //
					A_ + "King Edward", //
					B_ + "King Edward", //
					"King Edward", //
					A_ + "Pond Mills Rd", //
					"Pond Mills Rd", //
					A_ + "Pond Mills", //
					"Pond Mills" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Pond Mills", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Kipps & Adelaide", //
					B_ + "King Edward & Thompson", //
					"King Edward & Thompson", //
					A_ + "Pond Mills & Thompson", //
					"Pond Mills & Thompson", //
					A_ + "Kipps Ln", //
					B_ + "Kipps Ln", //
					"Kipps Ln" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Kipps Ln", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					A, //
					"Dundas & Highbury", //
					A_ + "Trafalgar Hts", //
					A_ + "Trafalgar Heigths", // !
					A_ + "Trafalgar & Hudson", //
					"Trafalgar Hts", //
					A_ + "Argyle Mall", // <>
					"Argyle Mall", // <>
					B_ + "Bonaventure", //
					"Bonaventure", //
					"Argyle Mall" + SLASH + "Bonaventure" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall" + SLASH + "Bonaventure", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Argyle Mall", // <>
					B_ + "Argyle Mall", //
					A, //
					A_ + "Hale & Brydges", //
					A_ + "Natural Science", //
					B_ + "Natural Science", //
					"Natural Science" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Natural Science", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 3L) {
			if (Arrays.asList( //
					A_ + "Highbury & Hamilton", //
					B_ + "Highbury & Hamilton", //
					"Highbury & Hamilton", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Argyle Mall", //
					B_ + "Argyle Mall", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					B_ + "Southdale & Ernest", //
					"Southdale & Ernest", //
					A_ + "Fanshawe College", //
					B_ + "Fanshawe College", //
					"Fanshawe College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Fanshawe College", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "White Oaks Mall", //
					B_ + "White Oaks Mall", //
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					"Springbank & Wonderland", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Springbank & Wonderland", //
					A_ + "Downtown", // Only
					A_ + "Argyle Mall", //
					B_ + "Argyle Mall", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Byron", //
					B_ + "Byron", //
					"Byron" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Byron", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					"Richmond & Dundas", //
					"Parkwood", //
					"Parkwood Hosp" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Parkwood Hosp", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Baseline & Fairview", //
					"Natural Science" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Natural Science", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					"Brydges & Highbury", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"Wonderland & Blackacres", //
					C_ + "Sarnia & Wonderland", //
					"Sarnia & Wonderland", //
					C_ + "Natural Science", //
					"Natural Science", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Whitehills", //
					B_ + "Whitehills", //
					C_ + "Whitehills", //
					"Whitehills" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Whitehills", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					"Highbury/Brydges", // <>
					"White Oaks Mall", //
					"Natural Science", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Highbury & Trafalgar", //
					"Barker & Huron", //
					"Barker/Huron" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Barker/Huron", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 12L) {
			if (Arrays.asList( //
					"Wharncliffe & Wonderland", //
					"Commissioners & Wharncliffe", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 13L) {
			if (Arrays.asList( //
					A_ + "White Oaks Mall", //
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Masonville Mall", //
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					"Highbury & Brydges", // <>
					"Huron & Barker" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Huron & Barker", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Highbury & Brydges", // <>
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					B_ + "Cranbrook & Commissioners", //
					A_ + "Dalhousie & Brixham", //
					B_ + "Cranbrook", //
					A_ + "Downtown", //
					A + "- Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					B_ + "Cranbrook & Commissioners", //
					A_ + "Dalhousie & Brixham", //
					A_ + "Oakville & Huron", //
					B_ + "Oakville & Huron", //
					"Oakville & Huron" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Oakville & Huron", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Highbury & Huron", //
					"Westmount", //
					A_ + "Westmount Mall", //
					B_ + "Westmount Mall", //
					"Westmount Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westmount Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					"Commissioners & Deveron", //
					"Deveron @ Commissioners", //
					"Masonville Mall", //
					"Adelaide & King", //
					A_ + "Pond Mills", //
					"Pond Mills", //
					"Pond Mills & Summerside", //
					B_ + "Summerside", // <>
					"Summerside" // <>
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Summerside", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Summerside", // <>
					"Commissioners & Highbury", //
					A_ + "Commissioners @ Deveron", //
					"Commissioners @ Deveron", //
					B_ + "Masonville", //
					"Masonville", //
					A_ + "Masonville Mall", //
					B_ + "Masonville Mall", //
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 17L) {
			if (Arrays.asList( //
					A_ + "Oxford & Wonderland", //
					A_ + "Fanshawe College", //
					A_ + "Boler & Commissioners", //
					"Boler & Commissioners", //
					A_ + "Argyle Mall", //
					B_ + "Argyle Mall", //
					"Fanshawe College", //
					"Oxford & Wonderland", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Fanshawe College", //
					B_ + "Riverbend", //
					"Riverbend", //
					A_ + "Byron",//
					B_ + "Byron",//
					"Byron", //
					"Byron" + SLASH + "Riverbend" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Byron" + SLASH + "Riverbend", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 19L) {
			if (Arrays.asList( //
					"Fanshawe & Hastings Vis Hyde Pk", //
					"Downtown", //
					"Stoney Crk" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Stoney Crk", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Fanshawe & North Ctr", //
					"Hyde Pk Power Ctr", // <>
					"YMCA", //
					"King @ Clarence", //
					"Wellington & Dundas", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					"Beaverbrook & Wonderland", //
					"Downtown", //
					"Richmond & Dundas", //
					"Fanshawe College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Fanshawe College", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 21L) {
			if (Arrays.asList( //
					"Huron & Highbury", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 24L) {
			if (Arrays.asList( //
					"Westmount Mall", // <>
					"Commissioners @ Meadowlily", //
					"Talbot Vlg" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Talbot Vlg", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Westmount Mall", // <>
					"Victoria Hosp" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Victoria Hosp", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Commissioners @ Highbury", //
					"Summerside" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Summerside", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 26L) {
			if (Arrays.asList( //
					"Jalna & Bradley", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 28L) {
			if (Arrays.asList( //
					"Wonderland & Wharncliffe", //
					"Westmount Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westmount Mall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Exeter & Wonderland", //
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 29L) {
			if (Arrays.asList( //
					"Natural Sciences", // <>
					"Capulet Ln" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Capulet Ln", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (Arrays.asList( //
					"Adelaide & Huron", //
					"Alumni Hall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Alumni Hall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					"Proudfoot & Oxford", //
					"Alumni Hall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Alumni Hall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 34L) {
			if (Arrays.asList( //
					"Garage", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Alumni Hall", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (Arrays.asList( //
					"Argyle Mall", // <>
					"Trafalgar Hts" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Trafalgar Hts", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Royal & Castle", //
					"Argyle Mall" // <>
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (Arrays.asList( //
					"Sovereign & Trafalgar", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					"Fanshawe & Adelaide", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Northridge & Grenfell", //
					"Northridge" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Northridge", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 90L) {
			if (Arrays.asList( //
					"Richmond & Dundas", // Only
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 93L) {
			if (Arrays.asList( //
					"Jalna @ Bradley", //
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Masonville Mall", // <>
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 102L) {
			if (Arrays.asList( //
					"Richmond @ King", //
					"King & Richmond", //
					"King & Clarence", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 104L) {
			if (Arrays.asList( //
					"Richmond & Dundas", //
					"Ridout & Grand" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ridout & Grand", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern AREA = Pattern.compile("((^|\\W)(area)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern ARGYLE_ = Pattern.compile("((^|\\W)(agyle)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ARGYLE_REPLACEMENT = "$2" + "Argyle" + "$4";

	private static final Pattern DEVERON_ = Pattern.compile("((^|\\W)(deverion)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DEVERON_REPLACEMENT = "$2" + "Deveron" + "$4";

	private static final Pattern INDUSTRIAL = Pattern.compile("((^|\\W)(industrial)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern HOSPITAL_ = Pattern.compile("((^|\\W)(hosptial|hos)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String HOSPITAL_REPLACEMENT = "$2" + HOSPITAL_SHORT + "$4";

	private static final Pattern MASONVILLE_ = Pattern.compile("((^|\\W)(masonvile|masvonille|masonvillel|masonville)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String MASONVILLE_REPLACEMENT = "$2" + MASONVILLE + "$4";

	private static final Pattern ONLY_ = Pattern.compile("((^|\\W)(only)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ONLY_REPLACEMENT = "$2" + EMPTY + "$4";

	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W)(univ western ontario|western university)(\\W|$))",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + UWO + "$4";

	private static final Pattern STARTS_WITH_EXPRESS_TO = Pattern.compile("(^express to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[\\s]?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(EMPTY); // 1st
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = STARTS_WITH_EXPRESS_TO.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = AREA.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = ARGYLE_.matcher(tripHeadsign).replaceAll(ARGYLE_REPLACEMENT);
		tripHeadsign = DEVERON_.matcher(tripHeadsign).replaceAll(DEVERON_REPLACEMENT);
		tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = HOSPITAL_.matcher(tripHeadsign).replaceAll(HOSPITAL_REPLACEMENT);
		tripHeadsign = MASONVILLE_.matcher(tripHeadsign).replaceAll(MASONVILLE_REPLACEMENT);
		tripHeadsign = ONLY_.matcher(tripHeadsign).replaceAll(ONLY_REPLACEMENT);
		tripHeadsign = UNIVERSITY_OF_WESTERN_ONTARIO.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{
				"NB", "SB", "WB", "EB",
				"SE", "SW", "NW", "NE",
				"VMP", "YMCA",
		};
	}

	private static final Pattern ENDS_WITH_STOP_CODE = Pattern.compile("( - #[\\d]*[\\w]*[']*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = ENDS_WITH_STOP_CODE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = HOSPITAL_.matcher(gStopName).replaceAll(HOSPITAL_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (!Utils.isDigitsOnly(stopId)) {
			if (!gStop.getStopCode().isEmpty()
					&& Utils.isDigitsOnly(gStop.getStopCode())) {
				return Integer.parseInt(gStop.getStopCode());
			}
			if ("DUFFWATS".equalsIgnoreCase(stopId)) {
				return 2836;
			}
			if ("WELLBAS3".equalsIgnoreCase(stopId)) {
				return 2434;
			}
			if ("SDALNIXO".equalsIgnoreCase(stopId)) {
				return 69;
			}
			if ("MCMAWON2".equalsIgnoreCase(stopId)) {
				return 2001;
			}
			if ("STACFANS".equalsIgnoreCase(stopId)) {
				return 3838;
			}
			if ("WESTLAM1".equalsIgnoreCase(stopId)) {
				return 2453;
			}
			throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop.toStringPlus());
		}
		return super.getStopId(gStop); // used by real-time API https://realtime.londontransit.ca/ // TODO GTFS-RT
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if ("'".equals(gStop.getStopCode())) {
			return EMPTY;
		}
		return super.getStopCode(gStop);
	}
}
