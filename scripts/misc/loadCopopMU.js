load('uuid.js');
load('coopRegion.js');

var ramsar = ["Apsley Marshes", "Ashmore Reef National Nature Reserve", "Banrock Station Wetland Complex", "Barmah Forest", "Becher Point Wetlands", "Blue Lake", "Bool And Hacks Lagoons", "Bowling Green Bay", "Cobourg Peninsula", "Coongie Lakes", "Corner Inlet", "Currawinya Lakes (Currawinya National Park)", "East Coast Cape Barren Island Lagoons", "Edithvale-Seaford Wetlands", "Eighty-Mile Beach", "Fivebough And Tuckerbil Swamps", "Flood Plain Lower Ringarooma River", "Forrestdale And Thomsons Lakes", "Ginini Flats Wetland Complex", "Gippsland Lakes", "Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)", "Gunbower Forest", "Gwydir Wetlands: Gingham And Lower Gwydir (Big Leather) Watercourses", "Hattah-Kulkyne Lakes", "Hosnies Spring", "Hunter Estuary Wetlands", "Interlaken (Lake Crescent)", "Jocks Lagoon", "Kakadu National Park", "Kerang Wetlands", "Lake Albacutya", "Lake Gore", "Lake Pinaroo (Fort Grey Basin)", "Lake Warden System", "Lakes Argyle And Kununurra", "Lavinia", "Little Llangothlin Nature Reserve", "Little Waterhouse Lake", "Logan Lagoon", "Moreton Bay", "Moulting Lagoon", "Muir-Byenup System", "Myall Lakes", "Narran Lake Nature Reserve", "Nsw Central Murray State Forests", "Ord River Floodplain", "Paroo River Wetlands", "Peel-Yalgorup System", "Piccaninnie Ponds Karst Wetlands", "Pitt Water-Orielton Lagoon", "Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Pulu Keeling National Park", "Riverland", "Roebuck Bay", "Shoalwater And Corio Bays Area (Shoalwater Bay Training Area, In Part - Corio Bay)", "The Coorong, And Lakes Alexandrina And Albert Wetland", "The Dales", "The Macquarie Marshes", "Toolibin Lake (Also Known As Lake Toolibin)", "Towra Point Nature Reserve", "Vasse-Wonnerup System", "Western District Lakes", "Western Port"]
var mus = ["Co-operative Management Area"];
var ramsarByMu = {};

var teCommunities = ["Alpine Sphagnum Bogs and Associated Fens", "Aquatic Root Mat Community 1 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 2 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 3 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 4 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community in Caves of the Swan Coastal Plain", "Arnhem Plateau Sandstone Shrubland Complex", "Assemblages of plants and invertebrate animals of tumulus (organic mound) springs of the Swan Coastal Plain", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Blue Gum High Forest of the Sydney Basin Bioregion", "Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Castlereagh Scribbly Gum and Agnes Banks Woodlands of the Sydney Basin Bioregion", "Central Hunter Valley eucalypt forest and woodland", "Clay Pans of the Swan Coastal Plain", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Cooks River/Castlereagh Ironbark Forest of the Sydney Basin Bioregion", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Cumberland Plain Shale Woodlands and Shale-Gravel Transition Forest", "Eastern Stirling Range Montane Heath and Thicket", "Eastern Suburbs Banksia Scrub of the Sydney Region", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Eucalyptus ovata - Callitris oblonga Forest", "Eyre Peninsula Blue Gum (Eucalyptus petiolaris) Woodland", "Giant Kelp Marine Forests of South East Australia", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Hunter Valley Weeping Myall (Acacia pendula) Woodland", "Illawarra and south coast lowland forest and woodland ecological community", "Iron-grass Natural Temperate Grassland of South Australia", "Kangaroo Island Narrow-leaved Mallee (Eucalyptus cneorifolia) Woodland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Grassy Woodland in the South East Corner Bioregion", "Lowland Native Grasslands of Tasmania", "Lowland Rainforest of Subtropical Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)", "Monsoon vine thickets on the coastal sand dunes of Dampier Peninsula", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Grasslands of the Murray Valley Plains", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Natural Temperate Grassland of the South Eastern Highlands", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Perched Wetlands of the Wheatbelt region with extensive stands of living sheoak and paperbark across the lake floor (Toolibin Lake)", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Scott River Ironstone Association", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Shrublands and Woodlands of the eastern Swan Coastal Plain", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Shrublands on southern Swan Coastal Plain ironstones", "Silurian Limestone Pomaderris Shrubland of the South East Corner and Australian Alps Bioregions", "Southern Highlands Shale Forest and Woodland in the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Swamp Tea-tree (Melaleuca irbyana) Forest of South-east Queensland", "Swamps of the Fleurieu Peninsula", "Temperate Highland Peat Swamps on Sandstone", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Thrombolite (microbial) community of coastal freshwater lakes of the Swan Coastal Plain (Lake Richmond)", "Thrombolite (microbialite) Community of a Coastal Brackish Lake (Lake Clifton)", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "Weeping Myall Woodlands", "Western Sydney Dry Rainforest and Moist Woodland on Shale", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"];
var teCommunitesByMu = {};

var worldHeritageSites = ["Fraser Island", "Gondwana Rainforests of Australia", "Great Barrier Reef", "Kakadu National Park", "Lord Howe Island Group", "Purnululu National Park", "Shark Bay, Western Australia", "Tasmanian Wilderness", "The Greater Blue Mountains Area", "The Ningaloo Coast", "Uluru - Kata Tjuta National Park", "Wet Tropics of Queensland", "Willandra Lakes Region"];
var worldHeritageSitesByMu = {

    "Co-operative Management Area": ["Wet Tropics of Queensland"],
};

var soilPriorities = ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"];
var soilPrioritiesByMU = {

    "Co-operative Management Area": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"]
};

var tssSpecies = ["Acacia purpureopetala", "Acacia whibleyana (Whibley Wattle)", "Amytornis woodwardi (White-throated Grasswren, Yirlinkirrkirr)", "Anthochaera phrygia (Regent Honeyeater)", "Banksia cuneata (Matchstick Banksia, Quairading Banksia)", "Banksia vincentia", "Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Brachychiton sp. Ormeau (L.H.Bird AQ435851) (Ormeau Bottle Tree)", "Burramys parvus (Mountain Pygmy-possum)", "Calyptorhynchus banksii graptogyne (Red-tailed Black-Cockatoo (south-eastern))", "Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Conilurus penicillatus (Brush-tailed Rabbit-rat, Brush-tailed Tree-rat, Pakooma)", "Cyanoramphus cookii (Norfolk Island Green Parrot, Tasman Parakeet, Norfolk Island Parakeet)", "Dasyornis brachypterus (Eastern Bristlebird)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Dasyurus viverrinus (Eastern Quoll, Luaner)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Epacris stuartii (Stuart's Heath, Southport Heath)", "Epthianura crocea tunneyi (Alligator Rivers Yellow Chat, Yellow Chat (Alligator Rivers))", "Eucalyptus crenulata (Silver Gum, Buxton Gum)", "Eucalyptus morrisbyi (Morrisby's Gum, Morrisbys Gum)", "Eucalyptus recurva (Mongarlowe Mallee)", "Fregata andrewsi (Christmas Island Frigatebird, Andrew's Frigatebird)", "Grevillea caleyi (Caley's Grevillea)", "Grevillea calliantha (Foote's Grevillea, Cataby Grevillea, Black Magic Grevillea)", "Gymnobelideus leadbeateri (Leadbeater's Possum)", "Hibiscus brennanii", "Homoranthus darwinioides", "Isoodon auratus auratus (Golden Bandicoot (mainland))", "Isoodon auratus barrowensis (Golden Bandicoot (Barrow Island))", "Lagorchestes hirsutus Central Australian subspecies (Mala, Rufous Hare-Wallaby (Central Australia))", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Lepidorrhachis mooreana (Little Mountain Palm, Moorei Palm)", "Lichenostomus melanops cassidix (Helmeted Honeyeater, Yellow-tufted Honeyeater (Helmeted))", "Livistona mariae subsp. mariae (Central Australian Cabbage Palm, Red Cabbage Palm)", "Macadamia jansenii (Bulberin Nut, Bulburin Nut Tree)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Myrmecodia beccarii (Ant Plant)", "Neophema chrysogaster (Orange-bellied Parrot)", "Ninox novaeseelandiae undulata (Norfolk Island Boobook, Southern Boobook (Norfolk Island))", "Notomys aquilo (Northern Hopping-mouse, Woorrentinta)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Oberonia attenuata (Mossman Fairy Orchid)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Perameles gunnii Victorian subspecies (Eastern Barred Bandicoot (Mainland))", "Perameles gunnii gunnii (Eastern Barred Bandicoot (Tasmania))", "Petaurus gracilis (Mahogany Glider)", "Petrogale lateralis MacDonnell Ranges race (Warru, Black-footed Rock-wallaby (MacDonnell Ranges race))", "Petrogale lateralis West Kimberley race (Black-footed Rock-wallaby (West Kimberley race))", "Petrogale lateralis hackettii (Recherche Rock-wallaby)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Pezoporus flaviventris (Western Ground Parrot, Kyloring)", "Pezoporus occidentalis (Night Parrot)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Potorous tridactylus gilbertii (Gilbert's Potoroo)", "Prasophyllum murfetii (Fleurieu Leek Orchid)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)", "Pteropus natalis (Christmas Island Flying-fox, Christmas Island Fruit-bat)", "Ptilotus fasciculatus (Fitzgerald's Mulla-mulla)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Sclerolaena napiformis (Turnip Copperburr)", "Sminthopsis aitkeni (Kangaroo Island Dunnart)", "Stipiturus mallee (Mallee Emu-wren)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)", "Syzygium paniculatum (Magenta Lilly Pilly, Magenta Cherry, Daguba, Scrub Cherry, Creek Lilly Pilly, Brush Cherry)", "Tetratheca gunnii (Shy Pinkbells, Shy Susan)", "Thelymitra cyanapicata (Blue Top Sun-orchid, Dark-tipped Sun-orchid)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))", "Verticordia spicata subsp. squamosa (Scaly-leaved Featherflower)", "Zyzomys pedunculatus (Central Rock-rat, Antina)"];

var tssSpeciesByMu = {
    "Co-operative Management Area": ["Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)"]
}


var now = ISODate();
var programStart = ISODate('2018-06-30T14:00:00Z');
var programEnd = ISODate('2023-06-30T13:59:59Z');

for (var i = 0; i < mus.length; i++) {
    var mu = {
        name: mus[i],
        managementUnitId: UUID.generate(),
        status: 'active',
        dateCreated: now,
        lastUpdated: now,
        startDate: programStart,
        endDate: programEnd,
        config: {

            "projectReports": [
                {
                    "reportType": "Activity",
                    "firstReportingPeriodEnd": "2018-09-30T14:00:00Z",
                    "reportDescriptionFormat": "Year %5$s - %6$s %7$d Outputs Report",
                    "reportNameFormat": "Year %5$s - %6$s %7$d Outputs Report",
                    "reportingPeriodInMonths": 3,
                    "description": "",
                    "category": "Outputs Reporting",
                    "activityType": "RLP Output Report",
                    "canSubmitDuringReportingPeriod": true,
                    "adjustmentActivityType": "RLP Output Report Adjustment"
                },
                {
                    "firstReportingPeriodEnd": "2019-06-30T14:00:00Z",
                    "reportType": "Administrative",
                    "reportDescriptionFormat": "Annual Progress Report %2$tY - %3$tY for %4$s",
                    "reportNameFormat": "Annual Progress Report %2$tY - %3$tY",
                    "reportingPeriodInMonths": 12,
                    "description": "",
                    "category": "Annual Progress Reporting",
                    "activityType": "RLP Annual Report"
                },
                {
                    "reportType": "Single",
                    "reportDescriptionFormat": "Outcomes Report 1 for %4$s",
                    "reportNameFormat": "Outcomes Report 1",
                    "reportingPeriodInMonths": 36,
                    "multiple": false,
                    "description": "_Please note that the reporting fields for these reports are currently being developed_",
                    "category": "Outcomes Report 1",
                    "reportsAlignedToCalendar": false,
                    "activityType": "RLP Short term project outcomes",
                    "firstReportingPeriodEnd": "2021-06-30T14:00:00Z"
                },
                {
                    "reportType": "Single",
                    "reportDescriptionFormat": "Outcomes Report 2 for %4$s",
                    "reportNameFormat": "Outcomes Report 2",
                    "reportingPeriodInMonths": 0,
                    "multiple": false,
                    "description": "_Please note that the reporting fields for these reports are currently being developed_",
                    "minimumPeriodInMonths": 37,
                    "category": "Outcomes Report 2",
                    "reportsAlignedToCalendar": false,
                    "activityType": "RLP Medium term project outcomes",
                    "firstReportingPeriodEnd": "2023-06-30T14:00:00Z"
                }
            ],
            "managementUnitReports": [
                {
                    "reportType": "Administrative",
                    "firstReportingPeriodEnd": "2018-09-30T14:00:00Z",
                    "reportDescriptionFormat": "Core services report %d for %4$s",
                    "reportNameFormat": "Core services report %d",
                    "reportingPeriodInMonths": 3,
                    "category": "Core Services Reporting",
                    "activityType": "RLP Core Services report"
                },
                {
                    "firstReportingPeriodEnd": "2019-06-30T14:00:00Z",
                    "reportType": "Administrative",
                    "reportDescriptionFormat": "Core services annual report %d for %4$s",
                    "reportNameFormat": "Core services annual report %d",
                    "reportingPeriodInMonths": 12,
                    "category": "Core Services Annual Reporting",
                    "activityType": "RLP Core Services annual report"
                }
            ]
        }

    };

    mu.priorities = [];
    var priorities = ramsarByMu[mus[i]] || [];
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Ramsar', priority: priorities[j]})
    }
    ;
    priorities = tssSpeciesByMu[mus[i]] || [];
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Threatened Species', priority: priorities[j]})
    }
    ;
    priorities = teCommunitesByMu[mus[i]] || [];
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Threatened Ecological Communities', priority: priorities[j]})
    }
    ;
    priorities = worldHeritageSitesByMu[mus[i]] || [];
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'World Heritage Sites', priority: priorities[j]})
    }
    ;
    priorities = soilPrioritiesByMU[mus[i]] || [];
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Soil Quality', priority: priorities[j]})
    }
    ;

    var landManagementPriorities = ['Soil acidification', 'Soil erosion', 'Hillslope erosion', 'Wind erosion', 'Native vegetation and biodiversity on-farm'];
    for (var j = 0; j < landManagementPriorities.length; j++) {
        mu.priorities.push({category: 'Land Management', priority: landManagementPriorities[j]});
    }
    mu.priorities.push({category: 'Sustainable Agriculture', priority: 'Climate change adaptation'});
    mu.priorities.push({category: 'Sustainable Agriculture', priority: 'Market traceability'});


    mu.outcomes = [
        {
            outcome: "1. By 2023, there is restoration of, and reduction in threats to, the ecological character of Ramsar sites, through the implementation of priority actions",
            priorities: [{category: "Ramsar"}],
            category: "environment"
        },
        {
            outcome: "2. By 2023, the trajectory of species targeted under the Threatened Species Strategy, and other EPBC Act priority species, is stabilised or improved.",
            priorities: [{category: "Threatened Species"}],
            category: "environment"
        },
        {
            outcome: "3. By 2023, invasive species management has reduced threats to the natural heritage Outstanding Universal Value of World Heritage properties through the implementation of priority actions.",
            priorities: [{category: "World Heritage Sites"}],
            category: "environment"
        },
        {
            outcome: "4. By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities.",
            priorities: [{category: "Threatened Ecological Communities"}],
            category: "environment"
        },
        {
            outcome: "5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.",
            priorities: [{category: "Land Management"}],
            category: "agriculture"
        },
        {
            outcome: "6. By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production.",
            priorities: [{category: "Sustainable Agriculture"}],
            category: "agriculture"
        }
    ];


    var existingMu = db.managementUnit.find({name: mu.name});
    if (existingMu.hasNext()) {
        var mu2 = existingMu.next();
        mu.description = mu2.description;
        mu._id = mu2._id;
        mu.programId = mu2.programId;

    }





    var now = new Date();
    var site = {
        name:mu.name,
        type:'programArea',
        dateCreated: now,
        lastUpdated: now,
        extent: {
            geometry: coopRegion,
            source:"upload"
        },
        status:'active',
        siteId:UUID.generate()
    };
    site.extent.geometry.state = "QLD";
    db.site.insert(site);

    mu.managementUnitSiteId = site.siteId;
    db.managementUnit.save(mu);

}