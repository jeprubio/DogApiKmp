Pod::Spec.new do |spec|
    spec.name                     = 'libraryDogApiSwift'
    spec.version                  = '1.0'
    spec.homepage                 = 'https://telefonica.com'
    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
    spec.authors                  = ''
    spec.license                  = { :type => 'MIT', :file => 'LICENSE' }
    spec.summary                  = 'Some description for a Kotlin/Native module'
    spec.module_name              = "LibraryDogApiSwift"
    
    
    spec.static_framework         = true
    spec.dependency 'libraryDogApi'
    spec.source_files = "build/cocoapods/framework/LibraryDogApiSwift/**/*.{h,m,swift}"
end