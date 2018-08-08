load('uuid.js');

var ramsar = ["Apsley Marshes", "Ashmore Reef National Nature Reserve", "Banrock Station Wetland Complex", "Barmah Forest", "Becher Point Wetlands", "Blue Lake", "Bool And Hacks Lagoons", "Bowling Green Bay", "Cobourg Peninsula", "Coongie Lakes", "Corner Inlet", "Currawinya Lakes (Currawinya National Park)", "East Coast Cape Barren Island Lagoons", "Edithvale-Seaford Wetlands", "Eighty-Mile Beach", "Fivebough And Tuckerbil Swamps", "Flood Plain Lower Ringarooma River", "Forrestdale And Thomsons Lakes", "Ginini Flats Wetland Complex", "Gippsland Lakes", "Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)", "Gunbower Forest", "Gwydir Wetlands: Gingham And Lower Gwydir (Big Leather) Watercourses", "Hattah-Kulkyne Lakes", "Hosnies Spring", "Hunter Estuary Wetlands", "Interlaken (Lake Crescent)", "Jocks Lagoon", "Kakadu National Park", "Kerang Wetlands", "Lake Albacutya", "Lake Gore", "Lake Pinaroo (Fort Grey Basin)", "Lake Warden System", "Lakes Argyle And Kununurra", "Lavinia", "Little Llangothlin Nature Reserve", "Little Waterhouse Lake", "Logan Lagoon", "Moreton Bay", "Moulting Lagoon", "Muir-Byenup System", "Myall Lakes", "Narran Lake Nature Reserve", "Nsw Central Murray State Forests", "Ord River Floodplain", "Paroo River Wetlands", "Peel-Yalgorup System", "Piccaninnie Ponds Karst Wetlands", "Pitt Water-Orielton Lagoon", "Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Pulu Keeling National Park", "Riverland", "Roebuck Bay", "Shoalwater And Corio Bays Area (Shoalwater Bay Training Area, In Part - Corio Bay)", "The Coorong, And Lakes Alexandrina And Albert Wetland", "The Dales", "The Macquarie Marshes", "Toolibin Lake (Also Known As Lake Toolibin)", "Towra Point Nature Reserve", "Vasse-Wonnerup System", "Western District Lakes", "Western Port"]
var mus = ["Central Tablelands", "Central West", "Greater Sydney", "Hunter", "Murray", "North Coast", "North Coast - Lord Howe Island", "North West NSW", "Northern Tablelands", "Riverina", "South East NSW", "Western", "Corangamite", "East Gippsland", "Glenelg Hopkins", "Goulburn Broken", "Mallee", "North Central", "North East", "Port Phillip and Western Port", "West Gippsland", "Wimmera", "Burnett Mary", "Cape York", "Condamine", "Co-operative Management Area", "Desert Channels", "Fitzroy", "Burdekin", "Northern Gulf", "Maranoa Balonne and Border Rivers", "Mackay Whitsunday", "South East Queensland", "South West Queensland", "Southern Gulf", "Wet Tropics", "Torres Strait", "Adelaide and Mount Lofty Ranges", "Alinytjara Wilurara", "Eyre Peninsula", "Kangaroo Island", "Northern and Yorke", "South Australian Arid Lands", "South Australian Murray Darling Basin", "South East", "Northern Agricultural Region", "Peel-Harvey Region", "Swan Region", "Rangelands Region", "South Coast Region", "South West Region", "Avon River Basin", "North West NRM Region", "North NRM Region", "South NRM Region", "South NRM Region - Macquarie Islands", "Northern Territory", "ACT"];
var ramsarByMu = {
    "South NRM Region": ["Apsley Marshes", "Interlaken (Lake Crescent)", "Moulting Lagoon", "Pitt Water-Orielton Lagoon"],
    "South Australian Murray Darling Basin": ["Banrock Station Wetland Complex", "Riverland", "The Coorong, And Lakes Alexandrina And Albert Wetland"],
    "Murray": ["Barmah Forest", "Blue Lake", "Gunbower Forest", "Nsw Central Murray State Forests"],
    "Goulburn Broken": ["Barmah Forest", "Nsw Central Murray State Forests"],
    "Swan Region": ["Becher Point Wetlands", "Forrestdale And Thomsons Lakes"],
    "South East NSW": ["Blue Lake"],
    "South East": ["Bool And Hacks Lagoons", "Piccaninnie Ponds Karst Wetlands", "The Coorong, And Lakes Alexandrina And Albert Wetland"],
    "Burdekin": ["Bowling Green Bay"],
    "Northern Territory": ["Cobourg Peninsula", "Kakadu National Park", "Lakes Argyle And Kununurra"],
    "Desert Channels": ["Coongie Lakes"],
    "South Australian Arid Lands": ["Coongie Lakes"],
    "West Gippsland": ["Corner Inlet", "Gippsland Lakes", "Western Port"],
    "Western": ["Currawinya Lakes (Currawinya National Park)", "Lake Pinaroo (Fort Grey Basin)", "Narran Lake Nature Reserve", "Paroo River Wetlands", "Riverland"],
    "South West Queensland": ["Currawinya Lakes (Currawinya National Park)"],
    "North NRM Region": ["East Coast Cape Barren Island Lagoons", "Flood Plain Lower Ringarooma River", "Jocks Lagoon", "Little Waterhouse Lake", "Logan Lagoon"],
    "Port Phillip and Western Port": ["Edithvale-Seaford Wetlands", "Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Western Port"],
    "Rangelands Region": ["Eighty-Mile Beach", "Lakes Argyle And Kununurra", "Ord River Floodplain", "Roebuck Bay"],
    "Riverina": ["Fivebough And Tuckerbil Swamps", "Ginini Flats Wetland Complex"],
    "Peel-Harvey Region": ["Forrestdale And Thomsons Lakes", "Peel-Yalgorup System"],
    "ACT": ["Ginini Flats Wetland Complex"],
    "East Gippsland": ["Gippsland Lakes"],
    "Burnett Mary": ["Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)"],
    "South East Queensland": ["Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)", "Moreton Bay"],
    "North Central": ["Gunbower Forest", "Kerang Wetlands", "Nsw Central Murray State Forests"],
    "North West NSW": ["Gwydir Wetlands: Gingham And Lower Gwydir (Big Leather) Watercourses", "Narran Lake Nature Reserve"],
    "Mallee": ["Hattah-Kulkyne Lakes", "Riverland"],
    "Hunter": ["Hunter Estuary Wetlands", "Myall Lakes"],
    "Wimmera": ["Lake Albacutya"],
    "South Coast Region": ["Lake Gore", "Lake Warden System", "Muir-Byenup System"],
    "North West NRM Region": ["Lavinia"],
    "Northern Tablelands": ["Little Llangothlin Nature Reserve"],
    "South West Region": ["Muir-Byenup System", "Toolibin Lake (Also Known As Lake Toolibin)", "Vasse-Wonnerup System"],
    "Glenelg Hopkins": ["Piccaninnie Ponds Karst Wetlands", "Western District Lakes"],
    "Corangamite": ["Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Western District Lakes"],
    "Fitzroy": ["Shoalwater And Corio Bays Area (Shoalwater Bay Training Area, In Part - Corio Bay)"],
    "Central West": ["The Macquarie Marshes"],
    "Greater Sydney": ["Towra Point Nature Reserve"]
};

var teCommunities = ["Alpine Sphagnum Bogs and Associated Fens", "Aquatic Root Mat Community 1 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 2 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 3 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 4 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community in Caves of the Swan Coastal Plain", "Arnhem Plateau Sandstone Shrubland Complex", "Assemblages of plants and invertebrate animals of tumulus (organic mound) springs of the Swan Coastal Plain", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Blue Gum High Forest of the Sydney Basin Bioregion", "Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Castlereagh Scribbly Gum and Agnes Banks Woodlands of the Sydney Basin Bioregion", "Central Hunter Valley eucalypt forest and woodland", "Clay Pans of the Swan Coastal Plain", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Cooks River/Castlereagh Ironbark Forest of the Sydney Basin Bioregion", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Cumberland Plain Shale Woodlands and Shale-Gravel Transition Forest", "Eastern Stirling Range Montane Heath and Thicket", "Eastern Suburbs Banksia Scrub of the Sydney Region", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Eucalyptus ovata - Callitris oblonga Forest", "Eyre Peninsula Blue Gum (Eucalyptus petiolaris) Woodland", "Giant Kelp Marine Forests of South East Australia", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Hunter Valley Weeping Myall (Acacia pendula) Woodland", "Illawarra and south coast lowland forest and woodland ecological community", "Iron-grass Natural Temperate Grassland of South Australia", "Kangaroo Island Narrow-leaved Mallee (Eucalyptus cneorifolia) Woodland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Grassy Woodland in the South East Corner Bioregion", "Lowland Native Grasslands of Tasmania", "Lowland Rainforest of Subtropical Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)", "Monsoon vine thickets on the coastal sand dunes of Dampier Peninsula", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Grasslands of the Murray Valley Plains", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Natural Temperate Grassland of the South Eastern Highlands", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Perched Wetlands of the Wheatbelt region with extensive stands of living sheoak and paperbark across the lake floor (Toolibin Lake)", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Scott River Ironstone Association", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Shrublands and Woodlands of the eastern Swan Coastal Plain", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Shrublands on southern Swan Coastal Plain ironstones", "Silurian Limestone Pomaderris Shrubland of the South East Corner and Australian Alps Bioregions", "Southern Highlands Shale Forest and Woodland in the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Swamp Tea-tree (Melaleuca irbyana) Forest of South-east Queensland", "Swamps of the Fleurieu Peninsula", "Temperate Highland Peat Swamps on Sandstone", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Thrombolite (microbial) community of coastal freshwater lakes of the Swan Coastal Plain (Lake Richmond)", "Thrombolite (microbialite) Community of a Coastal Brackish Lake (Lake Clifton)", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "Weeping Myall Woodlands", "Western Sydney Dry Rainforest and Moist Woodland on Shale", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"];
var teCommunitesByMu = {
    "Murray": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Riverina": ["Alpine Sphagnum Bogs and Associated Fens", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East NSW": ["Alpine Sphagnum Bogs and Associated Fens", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Illawarra and south coast lowland forest and woodland ecological community", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Grassy Woodland in the South East Corner Bioregion", "Natural Temperate Grassland of the South Eastern Highlands", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Southern Highlands Shale Forest and Woodland in the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "East Gippsland": ["Alpine Sphagnum Bogs and Associated Fens", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Silurian Limestone Pomaderris Shrubland of the South East Corner and Australian Alps Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Goulburn Broken": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North East": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Port Phillip and Western Port": ["Alpine Sphagnum Bogs and Associated Fens", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "West Gippsland": ["Alpine Sphagnum Bogs and Associated Fens", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Damp Grassland of the Victorian Coastal Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North West NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "North NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Eucalyptus ovata - Callitris oblonga Forest", "Giant Kelp Marine Forests of South East Australia", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "South NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Eucalyptus ovata - Callitris oblonga Forest", "Giant Kelp Marine Forests of South East Australia", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "ACT": ["Alpine Sphagnum Bogs and Associated Fens", "Natural Temperate Grassland of the South Eastern Highlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South West Region": ["Aquatic Root Mat Community 1 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 2 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 3 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 4 in Caves of the Leeuwin Naturaliste Ridge", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Perched Wetlands of the Wheatbelt region with extensive stands of living sheoak and paperbark across the lake floor (Toolibin Lake)", "Scott River Ironstone Association", "Shrublands on southern Swan Coastal Plain ironstones", "Subtropical and Temperate Coastal Saltmarsh"],
    "Swan Region": ["Aquatic Root Mat Community in Caves of the Swan Coastal Plain", "Assemblages of plants and invertebrate animals of tumulus (organic mound) springs of the Swan Coastal Plain", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Shrublands and Woodlands of the eastern Swan Coastal Plain", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh", "Thrombolite (microbial) community of coastal freshwater lakes of the Swan Coastal Plain (Lake Richmond)"],
    "Northern Territory": ["Arnhem Plateau Sandstone Shrubland Complex"],
    "Northern Agricultural Region": ["Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh"],
    "Peel-Harvey Region": ["Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh", "Thrombolite (microbialite) Community of a Coastal Brackish Lake (Lake Clifton)"],
    "Greater Sydney": ["Blue Gum High Forest of the Sydney Basin Bioregion", "Castlereagh Scribbly Gum and Agnes Banks Woodlands of the Sydney Basin Bioregion", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Cooks River/Castlereagh Ironbark Forest of the Sydney Basin Bioregion", "Cumberland Plain Shale Woodlands and Shale-Gravel Transition Forest", "Eastern Suburbs Banksia Scrub of the Sydney Region", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Temperate Highland Peat Swamps on Sandstone", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Western Sydney Dry Rainforest and Moist Woodland on Shale", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North West NSW": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Northern Tablelands": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Western": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Burnett Mary": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Condamine": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Desert Channels": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Fitzroy": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Burdekin": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Maranoa Balonne and Border Rivers": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East Queensland": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "Swamp Tea-tree (Melaleuca irbyana) Forest of South-east Queensland", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South West Queensland": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Cape York": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)"],
    "Northern Gulf": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"],
    "Mackay Whitsunday": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions"],
    "Wet Tropics": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)"],
    "Mallee": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North Central": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Wimmera": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Hunter": ["Central Hunter Valley eucalypt forest and woodland", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Hunter Valley Weeping Myall (Acacia pendula) Woodland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Subtropical and Temperate Coastal Saltmarsh", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South Coast Region": ["Clay Pans of the Swan Coastal Plain", "Eastern Stirling Range Montane Heath and Thicket", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Avon River Basin": ["Clay Pans of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia"],
    "Central West": ["Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Rangelands Region": ["Eucalypt Woodlands of the Western Australian Wheatbelt", "Monsoon vine thickets on the coastal sand dunes of Dampier Peninsula", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Eyre Peninsula": ["Eyre Peninsula Blue Gum (Eucalyptus petiolaris) Woodland", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Corangamite": ["Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Glenelg Hopkins": ["Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Central Tablelands": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Temperate Highland Peat Swamps on Sandstone", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Adelaide and Mount Lofty Ranges": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "Swamps of the Fleurieu Peninsula"],
    "Northern and Yorke": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "South Australian Murray Darling Basin": ["Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "Swamps of the Fleurieu Peninsula"],
    "Kangaroo Island": ["Kangaroo Island Narrow-leaved Mallee (Eucalyptus cneorifolia) Woodland", "Subtropical and Temperate Coastal Saltmarsh"],
    "North Coast": ["Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South Australian Arid Lands": ["Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"],
    "Southern Gulf": ["The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"]
};

var worldHeritageSites = ["Fraser Island", "Gondwana Rainforests of Australia", "Great Barrier Reef", "Kakadu National Park", "Lord Howe Island Group", "Purnululu National Park", "Shark Bay, Western Australia", "Tasmanian Wilderness", "The Greater Blue Mountains Area", "The Ningaloo Coast", "Uluru - Kata Tjuta National Park", "Wet Tropics of Queensland", "Willandra Lakes Region"];
var worldHeritageSitesByMu = {
    "Burnett Mary": ["Fraser Island", "Great Barrier Reef"],
    "Hunter": ["Gondwana Rainforests of Australia", "The Greater Blue Mountains Area"],
    "North Coast": ["Gondwana Rainforests of Australia"],
    "Northern Tablelands": ["Gondwana Rainforests of Australia"],
    "Condamine": ["Gondwana Rainforests of Australia"],
    "South East Queensland": ["Gondwana Rainforests of Australia"],
    "Cape York": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Fitzroy": ["Great Barrier Reef"],
    "Burdekin": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Mackay Whitsunday": ["Great Barrier Reef"],
    "Wet Tropics": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Torres Strait": ["Great Barrier Reef"],
    "Northern Territory": ["Kakadu National Park", "Uluru - Kata Tjuta National Park"],
    "North Coast - Lord Howe Island": ["Lord Howe Island Group"],
    "Rangelands Region": ["Purnululu National Park", "Shark Bay, Western Australia", "The Ningaloo Coast"],
    "Northern Agricultural Region": ["Shark Bay, Western Australia"],
    "North West NRM Region": ["Tasmanian Wilderness"],
    "North NRM Region": ["Tasmanian Wilderness"],
    "South NRM Region": ["Tasmanian Wilderness"],
    "Central Tablelands": ["The Greater Blue Mountains Area"],
    "Greater Sydney": ["The Greater Blue Mountains Area"],
    "South East NSW": ["The Greater Blue Mountains Area"],
    "Co-operative Management Area": ["Wet Tropics of Queensland"],
    "Northern Gulf": ["Wet Tropics of Queensland"],
    "Western": ["Willandra Lakes Region"]
};

var soilPriorities = ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"];
var soilPrioritiesByMU = {
    "Central Tablelands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Central West": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Greater Sydney": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Hunter": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Murray": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North Coast": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North West NSW": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Tablelands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Riverina": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East NSW": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Western": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Corangamite": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "East Gippsland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Glenelg Hopkins": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Goulburn Broken": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Mallee": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North Central": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North East": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Port Phillip and Western Port": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "West Gippsland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Wimmera": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Burnett Mary": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Cape York": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Condamine": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Co-operative Management Area": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Desert Channels": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Fitzroy": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Burdekin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Gulf": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Maranoa Balonne and Border Rivers": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Mackay Whitsunday": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East Queensland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South West Queensland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Southern Gulf": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Wet Tropics": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Adelaide and Mount Lofty Ranges": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Alinytjara Wilurara": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Eyre Peninsula": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Kangaroo Island": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern and Yorke": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Australian Arid Lands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Australian Murray Darling Basin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Agricultural Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Peel-Harvey Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Swan Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Rangelands Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Coast Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South West Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Avon River Basin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North West NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Territory": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "ACT": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"]
};

var tssSpecies = ["Acacia purpureopetala", "Acacia whibleyana (Whibley Wattle)", "Amytornis woodwardi (White-throated Grasswren, Yirlinkirrkirr)", "Anthochaera phrygia (Regent Honeyeater)", "Banksia cuneata (Matchstick Banksia, Quairading Banksia)", "Banksia vincentia", "Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Brachychiton sp. Ormeau (L.H.Bird AQ435851) (Ormeau Bottle Tree)", "Burramys parvus (Mountain Pygmy-possum)", "Calyptorhynchus banksii graptogyne (Red-tailed Black-Cockatoo (south-eastern))", "Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Conilurus penicillatus (Brush-tailed Rabbit-rat, Brush-tailed Tree-rat, Pakooma)", "Cyanoramphus cookii (Norfolk Island Green Parrot, Tasman Parakeet, Norfolk Island Parakeet)", "Dasyornis brachypterus (Eastern Bristlebird)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Dasyurus viverrinus (Eastern Quoll, Luaner)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Epacris stuartii (Stuart's Heath, Southport Heath)", "Epthianura crocea tunneyi (Alligator Rivers Yellow Chat, Yellow Chat (Alligator Rivers))", "Eucalyptus crenulata (Silver Gum, Buxton Gum)", "Eucalyptus morrisbyi (Morrisby's Gum, Morrisbys Gum)", "Eucalyptus recurva (Mongarlowe Mallee)", "Fregata andrewsi (Christmas Island Frigatebird, Andrew's Frigatebird)", "Grevillea caleyi (Caley's Grevillea)", "Grevillea calliantha (Foote's Grevillea, Cataby Grevillea, Black Magic Grevillea)", "Gymnobelideus leadbeateri (Leadbeater's Possum)", "Hibiscus brennanii", "Homoranthus darwinioides", "Isoodon auratus auratus (Golden Bandicoot (mainland))", "Isoodon auratus barrowensis (Golden Bandicoot (Barrow Island))", "Lagorchestes hirsutus Central Australian subspecies (Mala, Rufous Hare-Wallaby (Central Australia))", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Lepidorrhachis mooreana (Little Mountain Palm, Moorei Palm)", "Lichenostomus melanops cassidix (Helmeted Honeyeater, Yellow-tufted Honeyeater (Helmeted))", "Livistona mariae subsp. mariae (Central Australian Cabbage Palm, Red Cabbage Palm)", "Macadamia jansenii (Bulberin Nut, Bulburin Nut Tree)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Myrmecodia beccarii (Ant Plant)", "Neophema chrysogaster (Orange-bellied Parrot)", "Ninox novaeseelandiae undulata (Norfolk Island Boobook, Southern Boobook (Norfolk Island))", "Notomys aquilo (Northern Hopping-mouse, Woorrentinta)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Oberonia attenuata (Mossman Fairy Orchid)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Perameles gunnii Victorian subspecies (Eastern Barred Bandicoot (Mainland))", "Perameles gunnii gunnii (Eastern Barred Bandicoot (Tasmania))", "Petaurus gracilis (Mahogany Glider)", "Petrogale lateralis MacDonnell Ranges race (Warru, Black-footed Rock-wallaby (MacDonnell Ranges race))", "Petrogale lateralis West Kimberley race (Black-footed Rock-wallaby (West Kimberley race))", "Petrogale lateralis hackettii (Recherche Rock-wallaby)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Pezoporus flaviventris (Western Ground Parrot, Kyloring)", "Pezoporus occidentalis (Night Parrot)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Potorous tridactylus gilbertii (Gilbert's Potoroo)", "Prasophyllum murfetii (Fleurieu Leek Orchid)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)", "Pteropus natalis (Christmas Island Flying-fox, Christmas Island Fruit-bat)", "Ptilotus fasciculatus (Fitzgerald's Mulla-mulla)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Sclerolaena napiformis (Turnip Copperburr)", "Sminthopsis aitkeni (Kangaroo Island Dunnart)", "Stipiturus mallee (Mallee Emu-wren)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)", "Syzygium paniculatum (Magenta Lilly Pilly, Magenta Cherry, Daguba, Scrub Cherry, Creek Lilly Pilly, Brush Cherry)", "Tetratheca gunnii (Shy Pinkbells, Shy Susan)", "Thelymitra cyanapicata (Blue Top Sun-orchid, Dark-tipped Sun-orchid)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))", "Verticordia spicata subsp. squamosa (Scaly-leaved Featherflower)", "Zyzomys pedunculatus (Central Rock-rat, Antina)"];

var tssSpeciesByMu = {
    "Northern Gulf": ["Acacia purpureopetala", "Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Myrmecodia beccarii (Ant Plant)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)"],
    "Wet Tropics": ["Acacia purpureopetala", "Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Myrmecodia beccarii (Ant Plant)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Oberonia attenuata (Mossman Fairy Orchid)", "Petaurus gracilis (Mahogany Glider)"],
    "Eyre Peninsula": ["Acacia whibleyana (Whibley Wattle)", "Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Northern Territory": ["Amytornis woodwardi (White-throated Grasswren, Yirlinkirrkirr)", "Conilurus penicillatus (Brush-tailed Rabbit-rat, Brush-tailed Tree-rat, Pakooma)", "Epthianura crocea tunneyi (Alligator Rivers Yellow Chat, Yellow Chat (Alligator Rivers))", "Hibiscus brennanii", "Isoodon auratus auratus (Golden Bandicoot (mainland))", "Lagorchestes hirsutus Central Australian subspecies (Mala, Rufous Hare-Wallaby (Central Australia))", "Leipoa ocellata (Malleefowl)", "Livistona mariae subsp. mariae (Central Australian Cabbage Palm, Red Cabbage Palm)", "Macrotis lagotis (Greater Bilby)", "Notomys aquilo (Northern Hopping-mouse, Woorrentinta)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petrogale lateralis MacDonnell Ranges race (Warru, Black-footed Rock-wallaby (MacDonnell Ranges race))", "Zyzomys pedunculatus (Central Rock-rat, Antina)"],
    "Central Tablelands": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Homoranthus darwinioides", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)"],
    "Central West": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Homoranthus darwinioides", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)"],
    "Greater Sydney": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Grevillea caleyi (Caley's Grevillea)", "Homoranthus darwinioides", "Lathamus discolor (Swift Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Syzygium paniculatum (Magenta Lilly Pilly, Magenta Cherry, Daguba, Scrub Cherry, Creek Lilly Pilly, Brush Cherry)"],
    "Hunter": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Homoranthus darwinioides", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Syzygium paniculatum (Magenta Lilly Pilly, Magenta Cherry, Daguba, Scrub Cherry, Creek Lilly Pilly, Brush Cherry)"],
    "Murray": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Pedionomus torquatus (Plains-wanderer)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Sclerolaena napiformis (Turnip Copperburr)"],
    "North Coast": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "North West NSW": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)"],
    "Northern Tablelands": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)"],
    "Riverina": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Pedionomus torquatus (Plains-wanderer)", "Sclerolaena napiformis (Turnip Copperburr)"],
    "South East NSW": ["Anthochaera phrygia (Regent Honeyeater)", "Banksia vincentia", "Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Dasyornis brachypterus (Eastern Bristlebird)", "Eucalyptus recurva (Mongarlowe Mallee)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)", "Syzygium paniculatum (Magenta Lilly Pilly, Magenta Cherry, Daguba, Scrub Cherry, Creek Lilly Pilly, Brush Cherry)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Corangamite": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "East Gippsland": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Goulburn Broken": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Eucalyptus crenulata (Silver Gum, Buxton Gum)", "Gymnobelideus leadbeateri (Leadbeater's Possum)", "Lathamus discolor (Swift Parrot)", "Pedionomus torquatus (Plains-wanderer)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Sclerolaena napiformis (Turnip Copperburr)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)"],
    "North Central": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Sclerolaena napiformis (Turnip Copperburr)"],
    "North East": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Burramys parvus (Mountain Pygmy-possum)", "Lathamus discolor (Swift Parrot)", "Pedionomus torquatus (Plains-wanderer)", "Sclerolaena napiformis (Turnip Copperburr)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)"],
    "Port Phillip and Western Port": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Eucalyptus crenulata (Silver Gum, Buxton Gum)", "Gymnobelideus leadbeateri (Leadbeater's Possum)", "Lathamus discolor (Swift Parrot)", "Lichenostomus melanops cassidix (Helmeted Honeyeater, Yellow-tufted Honeyeater (Helmeted))", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Perameles gunnii Victorian subspecies (Eastern Barred Bandicoot (Mainland))", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "West Gippsland": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Gymnobelideus leadbeateri (Leadbeater's Possum)", "Lathamus discolor (Swift Parrot)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Wimmera": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Calyptorhynchus banksii graptogyne (Red-tailed Black-Cockatoo (south-eastern))", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Sclerolaena napiformis (Turnip Copperburr)"],
    "Burnett Mary": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)", "Macadamia jansenii (Bulberin Nut, Bulburin Nut Tree)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "Condamine": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)"],
    "Maranoa Balonne and Border Rivers": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)"],
    "South East Queensland": ["Anthochaera phrygia (Regent Honeyeater)", "Botaurus poiciloptilus (Australasian Bittern)", "Brachychiton sp. Ormeau (L.H.Bird AQ435851) (Ormeau Bottle Tree)", "Dasyornis brachypterus (Eastern Bristlebird)", "Lathamus discolor (Swift Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "ACT": ["Anthochaera phrygia (Regent Honeyeater)", "Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Swainsona recta (Small Purple-pea, Mountain Swainson-pea, Small Purple Pea)"],
    "Peel-Harvey Region": ["Banksia cuneata (Matchstick Banksia, Quairading Banksia)", "Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)"],
    "Avon River Basin": ["Banksia cuneata (Matchstick Banksia, Quairading Banksia)", "Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Leipoa ocellata (Malleefowl)", "Myrmecobius fasciatus (Numbat)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Ptilotus fasciculatus (Fitzgerald's Mulla-mulla)"],
    "North West NRM Region": ["Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus viverrinus (Eastern Quoll, Luaner)", "Lathamus discolor (Swift Parrot)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Perameles gunnii gunnii (Eastern Barred Bandicoot (Tasmania))", "Tetratheca gunnii (Shy Pinkbells, Shy Susan)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "North NRM Region": ["Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus viverrinus (Eastern Quoll, Luaner)", "Lathamus discolor (Swift Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Perameles gunnii gunnii (Eastern Barred Bandicoot (Tasmania))", "Tetratheca gunnii (Shy Pinkbells, Shy Susan)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "South NRM Region": ["Bettongia gaimardi (Tasmanian Bettong, Eastern Bettong)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus viverrinus (Eastern Quoll, Luaner)", "Epacris stuartii (Stuart's Heath, Southport Heath)", "Eucalyptus morrisbyi (Morrisby's Gum, Morrisbys Gum)", "Lathamus discolor (Swift Parrot)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Perameles gunnii gunnii (Eastern Barred Bandicoot (Tasmania))", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Western": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Pedionomus torquatus (Plains-wanderer)"],
    "Northern Agricultural Region": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Grevillea calliantha (Foote's Grevillea, Cataby Grevillea, Black Magic Grevillea)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Ptilotus fasciculatus (Fitzgerald's Mulla-mulla)", "Verticordia spicata subsp. squamosa (Scaly-leaved Featherflower)"],
    "Swan Region": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)"],
    "Rangelands Region": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Conilurus penicillatus (Brush-tailed Rabbit-rat, Brush-tailed Tree-rat, Pakooma)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Isoodon auratus auratus (Golden Bandicoot (mainland))", "Isoodon auratus barrowensis (Golden Bandicoot (Barrow Island))", "Lagorchestes hirsutus Central Australian subspecies (Mala, Rufous Hare-Wallaby (Central Australia))", "Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petrogale lateralis MacDonnell Ranges race (Warru, Black-footed Rock-wallaby (MacDonnell Ranges race))", "Petrogale lateralis West Kimberley race (Black-footed Rock-wallaby (West Kimberley race))", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Pezoporus occidentalis (Night Parrot)"],
    "South Coast Region": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Leipoa ocellata (Malleefowl)", "Myrmecobius fasciatus (Numbat)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petrogale lateralis hackettii (Recherche Rock-wallaby)", "Petrogale lateralis lateralis (Black-flanked Rock-wallaby, Moororong, Black-footed Rock Wallaby)", "Pezoporus flaviventris (Western Ground Parrot, Kyloring)", "Potorous tridactylus gilbertii (Gilbert's Potoroo)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)"],
    "South West Region": ["Bettongia penicillata (Brush-tailed Bettong, Woylie)", "Botaurus poiciloptilus (Australasian Bittern)", "Dasyurus geoffroii (Chuditch, Western Quoll)", "Drakaea elastica (Glossy-leafed Hammer Orchid, Glossy-leaved Hammer Orchid,  Warty Hammer Orchid)", "Leipoa ocellata (Malleefowl)", "Myrmecobius fasciatus (Numbat)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pseudocheirus occidentalis (Western Ringtail Possum, Ngwayir, Womp, Woder, Ngoor, Ngoolangit)"],
    "Glenelg Hopkins": ["Botaurus poiciloptilus (Australasian Bittern)", "Calyptorhynchus banksii graptogyne (Red-tailed Black-Cockatoo (south-eastern))", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Perameles gunnii Victorian subspecies (Eastern Barred Bandicoot (Mainland))", "Pimelea spinescens subsp. spinescens (Plains Rice-flower, Spiny Rice-flower, Prickly Pimelea)", "Rutidosis leptorrhynchoides (Button Wrinklewort)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Mallee": ["Botaurus poiciloptilus (Australasian Bittern)", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pedionomus torquatus (Plains-wanderer)", "Stipiturus mallee (Mallee Emu-wren)"],
    "Fitzroy": ["Botaurus poiciloptilus (Australasian Bittern)", "Macadamia jansenii (Bulberin Nut, Bulburin Nut Tree)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "South West Queensland": ["Botaurus poiciloptilus (Australasian Bittern)", "Pedionomus torquatus (Plains-wanderer)"],
    "Adelaide and Mount Lofty Ranges": ["Botaurus poiciloptilus (Australasian Bittern)", "Leipoa ocellata (Malleefowl)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Prasophyllum murfetii (Fleurieu Leek Orchid)", "Thelymitra cyanapicata (Blue Top Sun-orchid, Dark-tipped Sun-orchid)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Kangaroo Island": ["Botaurus poiciloptilus (Australasian Bittern)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Sminthopsis aitkeni (Kangaroo Island Dunnart)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "South Australian Murray Darling Basin": ["Botaurus poiciloptilus (Australasian Bittern)", "Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Prasophyllum murfetii (Fleurieu Leek Orchid)", "Stipiturus mallee (Mallee Emu-wren)", "Thelymitra cyanapicata (Blue Top Sun-orchid, Dark-tipped Sun-orchid)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "South East": ["Botaurus poiciloptilus (Australasian Bittern)", "Calyptorhynchus banksii graptogyne (Red-tailed Black-Cockatoo (south-eastern))", "Lathamus discolor (Swift Parrot)", "Leipoa ocellata (Malleefowl)", "Neophema chrysogaster (Orange-bellied Parrot)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Stipiturus mallee (Mallee Emu-wren)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "Cape York": ["Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Myrmecodia beccarii (Ant Plant)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)"],
    "Co-operative Management Area": ["Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Psephotus chrysopterygius (Golden-shouldered Parrot, Alwal)"],
    "Burdekin": ["Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Macrotis lagotis (Greater Bilby)", "Myrmecodia beccarii (Ant Plant)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Petaurus gracilis (Mahogany Glider)"],
    "Torres Strait": ["Casuarius casuarius johnsonii (Southern Cassowary, Australian Cassowary, Double-wattled Cassowary)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "Alinytjara Wilurara": ["Leipoa ocellata (Malleefowl)", "Petrogale lateralis MacDonnell Ranges race (Warru, Black-footed Rock-wallaby (MacDonnell Ranges race))"],
    "Northern and Yorke": ["Leipoa ocellata (Malleefowl)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Thinornis rubricollis rubricollis (Hooded Plover (eastern))"],
    "South Australian Arid Lands": ["Leipoa ocellata (Malleefowl)", "Macrotis lagotis (Greater Bilby)", "Myrmecobius fasciatus (Numbat)", "Olearia pannosa subsp. pannosa (Silver Daisy-bush, Silver-leaved Daisy, Velvet Daisy-bush)", "Pedionomus torquatus (Plains-wanderer)", "Pezoporus occidentalis (Night Parrot)"],
    "North Coast - Lord Howe Island": ["Lepidorrhachis mooreana (Little Mountain Palm, Moorei Palm)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"],
    "Desert Channels": ["Macrotis lagotis (Greater Bilby)", "Pedionomus torquatus (Plains-wanderer)", "Pezoporus occidentalis (Night Parrot)"],
    "Southern Gulf": ["Macrotis lagotis (Greater Bilby)", "Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)", "Pezoporus occidentalis (Night Parrot)"],
    "Mackay Whitsunday": ["Numenius madagascariensis (Eastern Curlew, Far Eastern Curlew)"]
};


var now = ISODate();
var programStart = ISODate('2018-06-30T14:00:00Z');
var programEnd = ISODate('2023-06-30T13:59:59Z');
var rlpProgram = db.program.find({name: 'Regional Land Partnerships'});
if (!rlpProgram.hasNext()) {
    throw "Can't find RLP in the database.  Aborting."
}

var parentId = rlpProgram.next()._id;

var toAdd = ['Marine Natural Resource Management'];

for (var i = 0; i < toAdd.length; i++) {
    var mu = {
        name: toAdd[i],
        programId: UUID.generate(),
        status: 'active',
        dateCreated: now,
        lastUpdated: now,
        startDate: programStart,
        endDate: programEnd,
        parent: parentId,
        config: {
            "projectReports": [
                {
                    "category": "Outputs Reporting",
                    "reportType": "Activity",
                    "activityType": "RLP Output Report",
                    "reportNameFormat": "Outputs Report %d",
                    "reportDescriptionFormat": "Outputs Report %d for %4$s"
                },
                {
                    "category": "Annual Progress Reporting",
                    "firstReportingPeriodEnd": "2019-06-30T14:00:00Z",
                    "reportingPeriodInMonths": 12,
                    "reportType": "Administrative",
                    "activityType": "RLP Annual Report",
                    "reportNameFormat": "Annual Progress Report %2$tY - %3$tY",
                    "reportDescriptionFormat": "Annual Progress Report %2$tY - %3$tY for %4$s"
                },
                {
                    "category": "Outcomes Report 1",
                    "reportsAlignedToCalendar": false,
                    "reportingPeriodInMonths": 36,
                    "reportType": "Single",
                    "activityType": "RLP Short term project outcomes",
                    "reportNameFormat": "Outcomes Report 1",
                    "reportDescriptionFormat": "Outcomes Report 1 for %4$s",
                    "multiple": false
                },
                {
                    "category": "Outcomes Report 2",
                    "reportsAlignedToCalendar": false,
                    "reportingPeriodInMonths": 0,
                    "reportType": "Single",
                    "activityType": "RLP Medium term project outcomes",
                    "reportNameFormat": "Outcomes Report 2",
                    "reportDescriptionFormat": "Outcomes Report 2 for %4$s",
                    "multiple": false,
                    "minimumPeriodInMonths": 37
                }
            ],
            "programReports": [
                {
                    "reportDescriptionFormat": "Core services report %d for %4$s",
                    "category": "Core Services Reporting",
                    "reportType": "Administrative",
                    "reportNameFormat": "Core services report %d",
                    "activityType": "RLP Core Services report"
                },
                {
                    "reportDescriptionFormat": "Core services annual report %d for %4$s",
                    "firstReportingPeriodEnd": "2019-06-30T14:00:00Z",
                    "reportingPeriodInMonths": 12,
                    "category": "Core Services Annual Reporting",
                    "reportType": "Administrative",
                    "reportNameFormat": "Core services annual report %d",
                    "activityType": "RLP Core Services annual report"
                }
            ]
        }
    };

    mu.priorities = [];
    var priorities = ramsarByMu[mu.name] || ramsar;
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Ramsar', priority: priorities[j]})
    }
    ;
    priorities = tssSpeciesByMu[mu.name] || tssSpecies;
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Threatened Species', priority: priorities[j]})
    }
    ;
    priorities = teCommunitesByMu[mu.name] || teCommunities;
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'Threatened Ecological Communities', priority: priorities[j]})
    }
    ;
    priorities = worldHeritageSitesByMu[mu.name] || worldHeritageSites;
    for (var j = 0; j < priorities.length; j++) {
        mu.priorities.push({category: 'World Heritage Sites', priority: priorities[j]})
    }
    ;
    priorities = soilPrioritiesByMU[mu.name] || soilPriorities;
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


    var existingMu = db.program.find({name: mu.name});
    if (existingMu.hasNext()) {
        var mu2 = existingMu.next();
        mu.description = mu2.description;
        mu._id = mu2._id;
        mu.programId = mu2.programId;

        print("Updating: ");

    }
    else {
        print("Adding: ");
    }
    printjson(mu);
    db.program.save(mu);

    var userIds = ['6065', '6360', '29926', '7420', '6942', '8138'];

    for (var i=0; i<userIds.length; i++) {
        db.userPermission.insert({entityId:mu.programId, entityType:'au.org.ala.ecodata.Program', userId:userIds[i], accessLevel:'caseManager'});
    }


}